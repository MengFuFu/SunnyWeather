package com.sunnyweather.android.logic.network

import com.sunnyweather.android.SunnyWeatherApplication
import com.sunnyweather.android.logic.model.RegeoResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 高德逆地理编码 API
 */
interface AmapService {
    @GET("v3/geocode/regeo")
    fun getRegeo(@Query("key") key: String = SunnyWeatherApplication.AMAP_KEY,
                 @Query("location") location: String,
                 @Query("output") output: String = "json"
    ): Call<RegeoResponse>
}
