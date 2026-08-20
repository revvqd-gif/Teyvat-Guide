package com.teyvatmap.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

@Entity(tableName = "map_labels")
data class LabelEntity(
    @PrimaryKey @Json(name = "id") @ColumnInfo(name = "id") val id: Int,
    @Json(name = "name") @ColumnInfo(name = "name") val name: String,
    @Json(name = "icon") @ColumnInfo(name = "icon") val icon: String?,
    @Json(name = "parent_id") @ColumnInfo(name = "parent_id") val parentId: Int? = null
)

@Entity(tableName = "map_areas")
data class AreaEntity(
    @PrimaryKey @Json(name = "id") @ColumnInfo(name = "id") val id: Int,
    @Json(name = "name") @ColumnInfo(name = "name") val name: String,
    @Json(name = "l_x") @ColumnInfo(name = "l_x") val lx: Int,
    @Json(name = "l_y") @ColumnInfo(name = "l_y") val ly: Int,
    @Json(name = "r_x") @ColumnInfo(name = "r_x") val rx: Int,
    @Json(name = "r_y") @ColumnInfo(name = "r_y") val ry: Int
)

@Entity(tableName = "map_points")
data class PointEntity(
    @PrimaryKey @Json(name = "id") @ColumnInfo(name = "id") val id: Int,
    @Json(name = "label_id") @ColumnInfo(name = "label_id") val labelId: Int,
    @Json(name = "x_pos") @ColumnInfo(name = "x_pos") val xPos: Double,
    @Json(name = "y_pos") @ColumnInfo(name = "y_pos") val yPos: Double,
    @Json(name = "author_name") @ColumnInfo(name = "author_name") val authorName: String?,
    @Json(name = "ctime") @ColumnInfo(name = "ctime") val ctime: String?,
    @Json(name = "display_state") @ColumnInfo(name = "display_state") val displayState: Int?,
    @Json(name = "area_id") @ColumnInfo(name = "area_id") val areaId: Int?,
    @Json(name = "ext_attrs") @ColumnInfo(name = "ext_attrs") val extAttrs: String?,
    @Json(name = "ext_attrs_map") @ColumnInfo(name = "ext_attrs_map") val extAttrsMap: String?,
    @Json(name = "z_level") @ColumnInfo(name = "z_level") val zLevel: Int?,
    @Json(name = "icon_sign") @ColumnInfo(name = "icon_sign") val iconSign: Int?,
    @Json(name = "point_group") @ColumnInfo(name = "point_group") val pointGroup: String?
)

@Entity(tableName = "marked_points")
data class MarkedPointEntity(
    @PrimaryKey @Json(name = "point_id") @ColumnInfo(name = "point_id") val pointId: Int
)

@Entity(tableName = "map_info")
data class MapInfoEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = 1,
    @Json(name = "origin_x") @ColumnInfo(name = "origin_x") val originX: Int,
    @Json(name = "origin_y") @ColumnInfo(name = "origin_y") val originY: Int,
    @Json(name = "total_size_x") @ColumnInfo(name = "total_size_x") val totalSizeX: Int,
    @Json(name = "total_size_y") @ColumnInfo(name = "total_size_y") val totalSizeY: Int,
    @Json(name = "padding_left") @ColumnInfo(name = "padding_left") val paddingLeft: Int,
    @Json(name = "padding_top") @ColumnInfo(name = "padding_top") val paddingTop: Int,
    @Json(name = "min_zoom") @ColumnInfo(name = "min_zoom") val minZoom: Int,
    @Json(name = "max_zoom") @ColumnInfo(name = "max_zoom") val maxZoom: Int,
    @Json(name = "map_version") @ColumnInfo(name = "map_version") val mapVersion: String,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis()
)