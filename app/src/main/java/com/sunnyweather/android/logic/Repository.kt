package com.sunnyweather.android.logic

import androidx.lifecycle.liveData
import com.sunnyweather.android.logic.dao.PlaceDao
import com.sunnyweather.android.logic.model.Place
import com.sunnyweather.android.logic.model.Weather
import com.sunnyweather.android.logic.network.SunnyWeatherNetWork
import kotlinx.coroutines.Dispatchers

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




}