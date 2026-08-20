package com.teyvatmap.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(labels: List<LabelEntity>)

    @Query("SELECT * FROM map_labels WHERE parent_id IS NULL ORDER BY name")
    fun getTopLevelLabels(): Flow<List<LabelEntity>>

    @Query("SELECT * FROM map_labels WHERE parent_id = :parentId ORDER BY name")
    fun getChildLabels(parentId: Int): Flow<List<LabelEntity>>

    @Query("SELECT * FROM map_labels WHERE id IN (:labelIds)")
    suspend fun getLabelsByIds(labelIds: List<Int>): List<LabelEntity>

    @Query("DELETE FROM map_labels")
    suspend fun clearAll()
}

@Dao
interface AreaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(areas: List<AreaEntity>)

    @Query("SELECT * FROM map_areas")
    fun getAllAreas(): Flow<List<AreaEntity>>

    @Query("DELETE FROM map_areas")
    suspend fun clearAll()
}

@Dao
interface PointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<PointEntity>)

    @Query("SELECT * FROM map_points WHERE label_id IN (:labelIds)")
    fun getPointsByLabelIds(labelIds: List<Int>): Flow<List<PointEntity>>

    @Query("SELECT * FROM map_points WHERE id = :pointId")
    suspend fun getPointById(pointId: Int): PointEntity?

    @Query("DELETE FROM map_points")
    suspend fun clearAll()
}

@Dao
interface MarkedPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(marks: List<MarkedPointEntity>)

    @Query("SELECT point_id FROM marked_points")
    fun getAllMarkedPointIds(): Flow<Set<Int>>

    @Query("DELETE FROM marked_points")
    suspend fun clearAll()
}

@Dao
interface MapInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapInfo: MapInfoEntity)

    @Query("SELECT * FROM map_info WHERE id = 1")
    fun getMapInfo(): Flow<MapInfoEntity?>

    @Query("DELETE FROM map_info")
    suspend fun clearAll()
}