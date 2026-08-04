package com.sunnyweather.android.logic.network

import android.util.Log
import com.sunnyweather.android.logic.model.DailyResponse
import com.sunnyweather.android.logic.model.IpLocationResponse
import com.sunnyweather.android.logic.model.RealtimeResponse
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object SunnyWeatherNetWork {
    private const val TAG = "SunnyWeatherNetWork"

    private val placeService = ServiceCreator.create<PlaceService>()

    suspend fun searchPlaces(query: String) = placeService.searchPlaces(query).await()

    /**
     * 带 429 限流重试的 await，最多重试 3 次，每次等待 1.5 秒
     */
    private suspend fun <T> Call<T>.await(): T {
        var currentCall = this
        for (attempt in 0..2) {
            try {
                return currentCall.awaitOnce()
            } catch (e: RuntimeException) {
                if (e.message?.contains("code=429") == true && attempt < 2) {
                    Log.w(TAG, "Rate limited, retrying in 1.5s... (attempt ${attempt + 1})")
                    delay(1500)
                    @Suppress("UNCHECKED_CAST")
                    currentCall = currentCall.clone() as Call<T>
                } else {
                    throw e
                }
            }
        }
        throw RuntimeException("Rate limit retry exhausted after 3 attempts")
    }

    private suspend fun <T> Call<T>.awaitOnce(): T {
        return suspendCoroutine {
            continuation ->
            enqueue(object: Callback<T> {
                override fun onResponse(call: Call<T?>, response: Response<T?>) {
                    val body = response.body()
                    if(body != null) {
                        continuation.resume(body)
                    }
                    else {
                        val errorMsg = "response body is null, code=${response.code()}" +
                            ", errorBody=${response.errorBody()?.string()}"
                        Log.e(TAG, errorMsg)
                        continuation.resumeWithException(RuntimeException(errorMsg))
                    }
                }

                override fun onFailure(call: Call<T?>, t: Throwable) {
                    Log.e(TAG, "onFailure: ${t.message}", t)
                    continuation.resumeWithException(t)
                }
            })
        }
    }

    private val weatherService = ServiceCreator.create(WeatherService::class.java)

    suspend fun getDailyWeather(lng: String, lat: String): DailyResponse {
        Log.d(TAG, "getDailyWeather: lng=$lng, lat=$lat")
        return weatherService.getDailyWeather(lng, lat).await()
    }

    suspend fun getRealtimeWeather(lng: String, lat: String): RealtimeResponse {
        Log.d(TAG, "getRealtimeWeather: lng=$lng, lat=$lat")
        return weatherService.getRealtimeWeather(lng, lat).await()
    }

    //高德ip服务
    private val ipService = ServiceCreator.createAmap(IpService::class.java)

    suspend fun fetchIpLocation(ip: String? = null): IpLocationResponse {
        return ipService.getIpLocation(ip = ip).await()
    }

    //高德逆地理编码服务
    private val amapService = ServiceCreator.createAmap(AmapService::class.java)

    /**
     * 根据经纬度查询城市名（逆地理编码），返回 city 名称字符串
     */
    suspend fun fetchRegeoCity(lng: Double, lat: Double): String {
        val location = "$lng,$lat"
        val response = amapService.getRegeo(location = location).await()

        // 调试日志：打印原始响应关键字段
        Log.d(TAG, "fetchRegeoCity response: status=${response.status}, info=${response.info}")
        val comp = response.regeocode?.addressComponent
        Log.d(TAG, "fetchRegeoCity addressComponent: province=${comp?.province}, city=${comp?.city}")

        if (response.status != "1") {
            throw RuntimeException("逆地理编码失败: ${response.info}")
        }

        // 城市名优先 city，为空时尝试 province（直辖市）
        val city = comp?.city?.trim().orEmpty()
            .ifEmpty { comp?.province?.trim().orEmpty() }
        if (city.isEmpty()) {
            throw RuntimeException("逆地理编码未获取到城市名 (province=${comp?.province}, city=${comp?.city})")
        }
        return city
    }
}