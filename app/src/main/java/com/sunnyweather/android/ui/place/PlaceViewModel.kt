package com.sunnyweather.android.ui.place

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.sunnyweather.android.logic.Repository
import com.sunnyweather.android.logic.model.Place
import retrofit2.http.Query

class PlaceViewModel : ViewModel() {

    private val searchLiveData = MutableLiveData<String>()

    val placeList = ArrayList<Place>()

    val placeLiveData = searchLiveData.switchMap {
        query ->
        Repository.searchPlaces(query)
    }

    //自动定位触发源
    private val autoLocateTrigger = MutableLiveData<Boolean>()
    //自动定位结果
    val autoLocateResult = autoLocateTrigger.switchMap {
        Repository.autoLocateAndSearch()
    }

    fun searchPlaces(query: String) {
        searchLiveData.value = query
    }

    fun savePlace(place: Place) = Repository.savePlace(place)

    fun getSavedPlace() = Repository.getSavedPlace()

    fun isPlaceSaved() = Repository.isPlaceSaved()

    //触发自动定位
    fun autoLocate() {
        autoLocateTrigger.value = true
    }
}