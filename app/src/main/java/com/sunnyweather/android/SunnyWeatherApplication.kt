package com.sunnyweather.android

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

class SunnyWeatherApplication : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        const val TOKEN = "zyQeUeJqSLT5CXaj"

        const val AMAP_KEY = "59eb19038302ed79d495cf0e4a9c8516"
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}