package com.sunnyweather.android.logic.network

import com.sunnyweather.android.SunnyWeatherApplication
import com.sunnyweather.android.logic.model.IpLocationResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface IpService {
    @GET("v3/ip")
    fun getIpLocation(@Query("key") key: String = SunnyWeatherApplication.AMAP_KEY,
                      @Query("ip") ip: String? = null
// 不传则自动获取客户端公网 IP
    ): Call<IpLocationResponse>
}