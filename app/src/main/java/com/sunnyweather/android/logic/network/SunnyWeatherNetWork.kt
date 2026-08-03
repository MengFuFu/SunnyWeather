package com.sunnyweather.android.logic.network

import android.util.Log
import com.sunnyweather.android.logic.model.DailyResponse
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
}