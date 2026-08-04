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

    /**
     * 加上高德地图前缀地址
     */
    private const val AMAP_BASE_URL = "https://restapi.amap.com/"

    private val caiyunRetrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val amapRetrofit = Retrofit.Builder()
        .baseUrl(AMAP_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun <T> create(serviceClass: Class<T>): T = caiyunRetrofit.create(serviceClass)

    /** inline 内联函数
     * <reified T> 重点：具体化泛型
     */
    inline fun <reified T> create(): T = create(T::class.java)

    //新增用于高德API
    fun <T> createAmap(serviceClass: Class<T>): T = amapRetrofit.create(serviceClass)
    inline fun <reified T> createAmap(): T = createAmap(T::class.java)
}