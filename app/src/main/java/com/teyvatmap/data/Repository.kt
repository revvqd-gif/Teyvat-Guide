package com.teyvatmap.data

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.concurrent.TimeUnit

interface MapRepository {
    suspend fun getMapInfo(): Result<MapInfoDetail>
    suspend fun getLabelTree(): Result<List<LabelNode>>
    suspend fun getAreas(): Result<List<MapArea>>
    suspend fun getPoints(labelIds: List<Int>): Result<List<MapPoint>>
    suspend fun getMarkedPoints(): Result<Set<Int>>
    
    // Flow-based for reactive UI
    fun observeLabels(): Flow<List<LabelNode>>
    fun observeAreas(): Flow<List<MapArea>>
    fun observePoints(labelIds: List<Int>): Flow<List<MapPoint>>
    fun observeMarkedPoints(): Flow<Set<Int>>
    fun observeMapInfo(): Flow<MapInfoDetail?>
}

class MapRepositoryImpl(
    private val api: HoyoApiService,
    private val cookieManager: CookieManager,
    private val db: com.teyvatmap.data.db.TeyvatMapDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MapRepository {

    private val MAP_ID = 2
    private val APP_SN = "ys_obc"
    private val LANG = "en-us"
    private val MAP_VERSION = "4.5"

    override suspend fun getMapInfo(): Result<MapInfoDetail> = withContext(ioDispatcher) {
        try {
            // Try cache first
            val cached = db.mapInfoDao().getMapInfo().firstOrNull()
            if (cached != null) {
                val detail = MapInfoDetail(
                    id = cached.id,
                    name = "Teyvat",
                    detailJson = "",
                    detailV2 = MapDetailV2(
                        origin = listOf(cached.originX, cached.originY),
                        totalSize = listOf(cached.totalSizeX, cached.totalSizeY),
                        padding = listOf(cached.paddingLeft, cached.paddingTop),
                        mapVersion = cached.mapVersion,
                        minZoom = cached.minZoom,
                        maxZoom = cached.maxZoom
                    )
                )
                return@withContext Result.success(detail)
            }

            // Fetch from API
            val response = api.getMapInfo(MAP_ID, APP_SN, LANG, MAP_VERSION)
            if (response.isSuccessful && response.body()?.data != null) {
                val info = response.body()!!.data!!.info
                // Cache it
                val entity = com.teyvatmap.data.db.MapInfoEntity(
                    originX = info.detailV2.origin[0],
                    originY = info.detailV2.origin[1],
                    totalSizeX = info.detailV2.totalSize[0],
                    totalSizeY = info.detailV2.totalSize[1],
                    paddingLeft = info.detailV2.padding[0],
                    paddingTop = info.detailV2.padding[1],
                    minZoom = info.detailV2.minZoom,
                    maxZoom = info.detailV2.maxZoom,
                    mapVersion = info.detailV2.mapVersion
                )
                db.mapInfoDao().insert(entity)
                Result.success(info)
            } else {
                Result.failure(Exception("Failed to get map info: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLabelTree(): Result<List<LabelNode>> = withContext(ioDispatcher) {
        try {
            // Try cache first
            val cached = db.labelDao().getTopLevelLabels().firstOrNull()
            if (cached != null && cached.isNotEmpty()) {
                val children = db.labelDao().getChildLabels(cached[0].id).firstOrNull() ?: emptyList()
                return@withContext Result.success(cached)
            }

            // Fetch from API
            val response = api.getLabelTree(MAP_ID, APP_SN, LANG, MAP_VERSION)
            if (response.isSuccessful && response.body()?.data != null) {
                val tree = response.body()!!.data!!.tree
                // Cache all labels (flatten tree)
                val allLabels = flattenLabels(tree)
                db.labelDao().clearAll()
                db.labelDao().insertAll(allLabels)
                Result.success(tree)
            } else {
                Result.failure(Exception("Failed to get label tree: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAreas(): Result<List<MapArea>> = withContext(ioDispatcher) {
        try {
            val cached = db.areaDao().getAllAreas().firstOrNull()
            if (cached != null && cached.isNotEmpty()) {
                return@withContext Result.success(cached)
            }

            val response = api.getAreas(MAP_ID, APP_SN, LANG, MAP_VERSION)
            if (response.isSuccessful && response.body()?.data != null) {
                val areas = response.body()!!.data!!.list
                db.areaDao().clearAll()
                db.areaDao().insertAll(areas)
                Result.success(areas)
            } else {
                Result.failure(Exception("Failed to get areas: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPoints(labelIds: List<Int>): Result<List<MapPoint>> = withContext(ioDispatcher) {
        try {
            val cookie = cookieManager.getCookieSync()
            if (cookie.isBlank()) {
                return Result.failure(Exception("No cookie available"))
            }

            val labelIdsStr = labelIds.joinToString(",")
            val response = api.getPoints(MAP_ID, labelIdsStr, APP_SN, LANG, MAP_VERSION, cookie)

            if (response.isSuccessful && response.body()?.data != null) {
                val points = response.body()!!.data!!.pointList
                // Cache points
                val entities = points.map { toPointEntity(it) }
                db.pointDao().insertAll(entities)
                Result.success(points)
            } else {
                Result.failure(Exception("Failed to get points: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMarkedPoints(): Result<Set<Int>> = withContext(ioDispatcher) {
        try {
            val cookie = cookieManager.getCookieSync()
            if (cookie.isBlank()) {
                return Result.failure(Exception("No cookie available"))
            }

            val response = api.getMarkedPoints(MAP_ID, APP_SN, LANG, MAP_VERSION, cookie)

            if (response.isSuccessful && response.body()?.data != null) {
                val marks = response.body()!!.data!!.pointList.map { it.pointId }.toSet()
                // Cache marks
                val entities = marks.map { com.teyvatmap.data.db.MarkedPointEntity(it) }
                db.markedPointDao().clearAll()
                db.markedPointDao().insertAll(entities)
                Result.success(marks)
            } else {
                Result.failure(Exception("Failed to get marks: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeLabels(): Flow<List<LabelNode>> {
        return db.labelDao().getTopLevelLabels()
            .map { entities -> entities.map { toLabelNode(it) } }
            .distinctUntilChanged()
    }

    override fun observeAreas(): Flow<List<MapArea>> {
        return db.areaDao().getAllAreas()
            .map { entities -> entities.map { toMapArea(it) } }
            .distinctUntilChanged()
    }

    override fun observePoints(labelIds: List<Int>): Flow<List<MapPoint>> {
        return db.pointDao().getPointsByLabelIds(labelIds)
            .map { entities -> entities.map { toMapPoint(it) } }
            .distinctUntilChanged()
    }

    override fun observeMarkedPoints(): Flow<Set<Int>> {
        return db.markedPointDao().getAllMarkedPointIds()
            .distinctUntilChanged()
    }

    override fun observeMapInfo(): Flow<MapInfoDetail?> {
        return db.mapInfoDao().getMapInfo()
            .map { entity ->
                entity?.let {
                    MapInfoDetail(
                        id = it.id,
                        name = "Teyvat",
                        detailJson = "",
                        detailV2 = MapDetailV2(
                            origin = listOf(it.originX, it.originY),
                            totalSize = listOf(it.totalSizeX, it.totalSizeY),
                            padding = listOf(it.paddingLeft, it.paddingTop),
                            mapVersion = it.mapVersion,
                            minZoom = it.minZoom,
                            maxZoom = it.maxZoom
                        )
                    )
                }
            }
            .distinctUntilChanged()
    }

    private fun flattenLabels(nodes: List<LabelNode>, parentId: Int? = null): List<com.teyvatmap.data.db.LabelEntity> {
        val result = mutableListOf<com.teyvatmap.data.db.LabelEntity>()
        for (node in nodes) {
            result.add(com.teyvatmap.data.db.LabelEntity(
                id = node.id,
                name = node.name,
                icon = node.icon,
                parentId = parentId
            ))
            node.children?.let { children ->
                result.addAll(flattenLabels(children, node.id))
            }
        }
        return result
    }

    private fun toLabelNode(entity: com.teyvatmap.data.db.LabelEntity): LabelNode {
        return LabelNode(entity.id, entity.name, entity.icon, null)
    }

    private fun toMapArea(entity: com.teyvatmap.data.db.AreaEntity): MapArea {
        return MapArea(entity.id, entity.name, entity.lx, entity.ly, entity.rx, entity.ry)
    }

    private fun toPointEntity(point: MapPoint): com.teyvatmap.data.db.PointEntity {
        import com.squareup.moshi.Moshi
        import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter<Map<String, String>>(Map::class.java)
        val extAttrsMapJson = point.extAttrsMap?.let { adapter.toJson(it) }
        return com.teyvatmap.data.db.PointEntity(
            id = point.id,
            labelId = point.labelId,
            xPos = point.xPos,
            yPos = point.yPos,
            authorName = point.authorName,
            ctime = point.ctime,
            displayState = point.displayState,
            areaId = point.areaId,
            extAttrs = point.extAttrs,
            extAttrsMap = extAttrsMapJson,
            zLevel = point.zLevel,
            iconSign = point.iconSign,
            pointGroup = point.pointGroup
        )
    }

    private fun toMapPoint(entity: com.teyvatmap.data.db.PointEntity): MapPoint {
        import com.squareup.moshi.Moshi
        import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter<Map<String, String>>(Map::class.java)
        val extAttrsMap = entity.extAttrsMap?.let { adapter.fromJson(it) }
        return MapPoint(
            id = entity.id,
            labelId = entity.labelId,
            xPos = entity.xPos,
            yPos = entity.yPos,
            authorName = entity.authorName,
            ctime = entity.ctime,
            displayState = entity.displayState,
            areaId = entity.areaId,
            extAttrs = entity.extAttrs,
            extAttrsMap = extAttrsMap,
            zLevel = entity.zLevel,
            iconSign = entity.iconSign,
            pointGroup = entity.pointGroup
        )
    }
}