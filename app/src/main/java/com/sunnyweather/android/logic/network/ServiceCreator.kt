package com.sunnyweather.android.logic.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ServiceCreator {

    /**
     * 彩云天气所有接口共同的前缀地址：https://api.caiyunapp.com/
     * 之前 PlaceService 里面 @GET("v2/place")
     * 最终完整地址 = BASE_URL + v2/place
     */
    private const val BASE_URL = "https://api.caiyunapp.com/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun <T> create(serviceClass: Class<T>): T = retrofit.create(serviceClass)

    /** inline 内联函数
     * <reified T> 重点：具体化泛型
     */
    inline fun <reified T> create(): T = create(T::class.java)
}