package com.teyvatmap.data

import com.squareup.moshi.Json

// API Response wrappers
data class ApiResponse<T>(
    @Json(name = "retcode") val retcode: Int,
    @Json(name = "message") val message: String,
    @Json(name = "data") val data: T?
)

data class MapInfo(
    @Json(name = "info") val info: MapInfoDetail
)

data class MapInfoDetail(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "detail") val detailJson: String,
    @Json(name = "detail_v2") val detailV2: MapDetailV2
)

data class MapDetailV2(
    @Json(name = "origin") val origin: List<Int>,
    @Json(name = "total_size") val totalSize: List<Int>,
    @Json(name = "padding") val padding: List<Int>,
    @Json(name = "map_version") val mapVersion: String,
    @Json(name = "min_zoom") val minZoom: Int,
    @Json(name = "max_zoom") val maxZoom: Int
)

data class LabelTreeResponse(
    @Json(name = "tree") val tree: List<LabelNode>
)

data class LabelNode(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "icon") val icon: String?,
    @Json(name = "children") val children: List<LabelNode>?
)

data class AreasResponse(
    @Json(name = "list") val list: List<MapArea>
)

data class MapArea(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "l_x") val lx: Int,
    @Json(name = "l_y") val ly: Int,
    @Json(name = "r_x") val rx: Int,
    @Json(name = "r_y") val ry: Int
)

data class PointsResponse(
    @Json(name = "point_list") val pointList: List<MapPoint>,
    @Json(name = "label_list") val labelList: List<Int>
)

data class MapPoint(
    @Json(name = "id") val id: Int,
    @Json(name = "label_id") val labelId: Int,
    @Json(name = "x_pos") val xPos: Double,
    @Json(name = "y_pos") val yPos: Double,
    @Json(name = "author_name") val authorName: String?,
    @Json(name = "ctime") val ctime: String?,
    @Json(name = "display_state") val displayState: Int?,
    @Json(name = "area_id") val areaId: Int?,
    @Json(name = "ext_attrs") val extAttrs: String?,
    @Json(name = "ext_attrs_map") val extAttrsMap: Map<String, String>?,
    @Json(name = "z_level") val zLevel: Int?,
    @Json(name = "icon_sign") val iconSign: Int?,
    @Json(name = "point_group") val pointGroup: String?
)

data class MarksResponse(
    @Json(name = "point_list") val pointList: List<MarkPoint>
)

data class MarkPoint(
    @Json(name = "point_id") val pointId: Int
)