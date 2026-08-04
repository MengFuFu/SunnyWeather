package com.sunnyweather.android.logic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.liveData
import com.sunnyweather.android.SunnyWeatherApplication
import com.sunnyweather.android.logic.dao.PlaceDao
import com.sunnyweather.android.logic.model.Place
import com.sunnyweather.android.logic.model.Weather
import com.sunnyweather.android.logic.network.SunnyWeatherNetWork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object Repository {

    fun savePlace(place: Place) = PlaceDao.savePlace(place)

    fun getSavedPlace() = PlaceDao.getSavedPlace()

    fun isPlaceSaved() = PlaceDao.isPlaceSaved()

    fun searchPlaces(query: String) = liveData(Dispatchers.IO) {
        val result = try {
            val placeResponse = SunnyWeatherNetWork.searchPlaces(query)
            if(placeResponse.status == "ok") {
                val places = placeResponse.places
                Result.success(places)
            }
            else {
                Result.failure(RuntimeException("response status is ${placeResponse.status}"))
            }
        }
        catch (e: Exception) {
            Result.failure<List<Place>>(e)
        }
        emit(result)
    }

    fun refreshWeather(lng: String, lat: String) = liveData(Dispatchers.IO) {
        val result = try {
            // 429 限流重试由 await() 内部自动处理，此处无需额外延迟
            val realtimeResponse = SunnyWeatherNetWork.getRealtimeWeather(lng, lat)
            val dailyResponse = SunnyWeatherNetWork.getDailyWeather(lng, lat)
            if(realtimeResponse.status == "ok" && dailyResponse.status == "ok") {
                val weather = Weather(realtimeResponse.result.realtime, dailyResponse.result.daily)
                Result.success(weather)
            }
            else {
                Result.failure(RuntimeException("realtime response status is ${realtimeResponse.status}" +
                "daily response status is ${dailyResponse.status}"))
            }
        } catch (e: Exception) {
            Result.failure<Weather>(e)
        }
        emit(result)
    }


    /**
     * 自动定位并搜索城市
     * 返回 LiveData<Result<Place>>，在 IO 线程执行
     * 优先使用 IP 定位，失败时回退到 GPS + 逆地理编码
     */
    fun autoLocateAndSearch() = liveData(Dispatchers.IO) {
        val result = try {
            // 第一步：尝试 IP 定位
            val cityName = tryIpLocation()

            // 第二步：IP 失败或为空，尝试 GPS 备用方案
            val finalCityName = if (cityName.isNotEmpty()) {
                cityName
            } else {
                Log.w(TAG, "IP定位未获取到城市名，尝试GPS备用方案")
                tryGpsFallback() ?: throw RuntimeException("IP和GPS定位均无法获取到城市名")
            }

            // 第三步：用城市名搜索彩云天气地点
            val placeResponse = SunnyWeatherNetWork.searchPlaces(finalCityName)
            if (placeResponse.status != "ok" || placeResponse.places.isEmpty()) {
                throw RuntimeException("未搜索到该城市的天气数据: $finalCityName")
            }

            Result.success(placeResponse.places[0])
        }
        catch (e: Exception) {
            Log.e(TAG, "autoLocateAndSearch failed", e)
            Result.failure<Place>(e)
        }
        emit(result)
    }

    /**
     * 通过 IP 定位获取城市名，失败或不支持时返回空字符串
     */
    private suspend fun tryIpLocation(): String {
        return try {
            val response = SunnyWeatherNetWork.fetchIpLocation()
            if (response.status != "1") {
                Log.w(TAG, "IP定位失败: ${response.info} (infocode=${response.infocode})")
                return ""
            }
            response.city.ifEmpty { response.province }.trim()
        } catch (e: Exception) {
            Log.w(TAG, "IP定位异常: ${e.message}")
            ""
        }
    }

    /**
     * GPS 备用方案：先尝试缓存位置（30 分钟内有效），缓存过期则主动请求
     * 一次实时定位（10 秒超时），再调用高德逆地理编码转换为城市名。失败返回 null。
     */
    private suspend fun tryGpsFallback(): String? {
        val context = SunnyWeatherApplication.context
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "没有定位权限，跳过GPS备用方案")
            return null
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // 步骤1：先尝试近期缓存（30 分钟内），快且零功耗
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            try {
                val cached = locationManager.getLastKnownLocation(provider)
                if (cached != null && isRecent(cached)) {
                    Log.d(TAG, "GPS备用: 从 $provider 缓存获取到位置 (${cached.latitude}, ${cached.longitude})")
                    val city = tryRegeo(cached)
                    if (city != null) return city
                }
            } catch (_: SecurityException) {}
        }

        // 步骤2：缓存不可用，主动请求一次实时定位（10 秒超时）
        Log.d(TAG, "GPS备用: 无可用缓存，请求实时定位...")
        val freshLocation = requestFreshLocation(locationManager)
        if (freshLocation != null) {
            Log.d(TAG, "GPS备用: 实时定位获取到位置 (${freshLocation.latitude}, ${freshLocation.longitude})")
            return tryRegeo(freshLocation)
        }

        Log.w(TAG, "GPS备用: 所有定位方式均失败")
        return null
    }

    /** 缓存位置的时间是否在 30 分钟内 */
    private fun isRecent(location: Location): Boolean {
        val ageMs = System.currentTimeMillis() - location.time
        return ageMs < 30 * 60 * 1000L
    }

    /** 尝试用 Location 进行逆地理编码，失败返回 null */
    private suspend fun tryRegeo(location: Location): String? {
        return try {
            SunnyWeatherNetWork.fetchRegeoCity(location.longitude, location.latitude)
        } catch (e: Exception) {
            Log.w(TAG, "GPS备用: 逆地理编码失败: ${e.message}")
            null
        }
    }

    /** 请求一次实时定位，10 秒超时 */
    private suspend fun requestFreshLocation(manager: LocationManager): Location? {
        return withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine { cont ->
                var resumed = false
                val listener = object : LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        if (!resumed) {
                            resumed = true
                            manager.removeUpdates(this)
                            cont.resume(loc)
                        }
                    }

                    override fun onProviderDisabled(provider: String) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                }

                cont.invokeOnCancellation {
                    manager.removeUpdates(listener)
                }

                try {
                    // 同时向 GPS 和 Network 提供器请求更新
                    manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener)
                    manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener)
                } catch (e: SecurityException) {
                    cont.resume(null)
                }
            }
        }
    }

    private const val TAG = "Repository"




}