package com.sunnyweather.android.logic.model

import com.google.gson.annotations.SerializedName

//对应接口返回最外层 JSON 结构
/**
 * {
 *   "status": "ok",
 *   "places": [
 *     // 多个地点对象，对应下方 Place 类
 *     {...},
 *     {...}
 *   ]
 * }
 */
//data class：数据类，自动生成 toString、equals、copy，专门用来存网络 / 本地数据
data class PlaceResponse(val status: String, val places: List<Place>)

//地点实体 Place
/**
 * {
 *   "name": "西安",
 *   "location": { "lng": "108.9398", "lat": "34.2644" },
 *   "formatted_address": "陕西省西安市"
 * }
 */
data class Place(val name: String, val location: Location,
    @SerializedName("formatted_address") val address: String)


//经纬度嵌套类 Location
/**
 * "location": {
 *   "lng": "116.403874",
 *   "lat": "39.914885"
 * }
 */
data class Location(val lng: String, val lat: String)