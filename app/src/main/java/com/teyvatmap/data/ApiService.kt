package com.teyvatmap.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface HoyoApiService {

    @GET("v3/map/info")
    suspend fun getMapInfo(
        @Query("map_id") mapId: Int,
        @Query("app_sn") appSn: String,
        @Query("lang") lang: String,
        @Header("x-rpc-map_version") mapVersion: String
    ): Response<ApiResponse<MapInfo>>

    @GET("v1/map/label/tree")
    suspend fun getLabelTree(
        @Query("map_id") mapId: Int,
        @Query("app_sn") appSn: String,
        @Query("lang") lang: String,
        @Header("x-rpc-map_version") mapVersion: String
    ): Response<ApiResponse<LabelTreeResponse>>

    @GET("v1/map/get_area_pageLabel")
    suspend fun getAreas(
        @Query("map_id") mapId: Int,
        @Query("app_sn") appSn: String,
        @Query("lang") lang: String,
        @Header("x-rpc-map_version") mapVersion: String
    ): Response<ApiResponse<AreasResponse>>

    @GET("v3/map/point/list")
    suspend fun getPoints(
        @Query("map_id") mapId: Int,
        @Query("label_ids") labelIds: String,
        @Query("app_sn") appSn: String,
        @Query("lang") lang: String,
        @Header("x-rpc-map_version") mapVersion: String,
        @Header("Cookie") cookie: String
    ): Response<ApiResponse<PointsResponse>>

    @GET("v1/map/point/mark_map_point_list")
    suspend fun getMarkedPoints(
        @Query("map_id") mapId: Int,
        @Query("app_sn") appSn: String,
        @Query("lang") lang: String,
        @Header("x-rpc-map_version") mapVersion: String,
        @Header("Cookie") cookie: String
    ): Response<ApiResponse<MarksResponse>>
}