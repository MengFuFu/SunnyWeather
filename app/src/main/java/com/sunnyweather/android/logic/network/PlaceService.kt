package com.sunnyweather.android.logic.network

import com.sunnyweather.android.SunnyWeatherApplication
import com.sunnyweather.android.logic.model.PlaceResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit 规定：所有网络 API 都必须定义在 interface 接口中，不能写 class。
 * Retrofit 底层会自动动态生成这个接口的实现类，不用自己写网络请求底层代码。
 */

/**
 * @GET：标记这是 GET 请求
 * @Query：代表 URL 拼接查询参数
 * Call：网络请求返回包装类，用于发起、取消请求，接收响应数据
 */

interface PlaceService {
    @GET("v2/place?token=${SunnyWeatherApplication.TOKEN}&lang=zh_CN")
    fun searchPlaces(@Query("query") query: String): Call<PlaceResponse>
}