package com.sunnyweather.android.ui.weather

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.sunnyweather.android.R
import com.sunnyweather.android.databinding.ActivityWeatherBinding
import com.sunnyweather.android.logic.model.Weather
import com.sunnyweather.android.logic.model.getSky

class WeatherActivity : AppCompatActivity() {

    val viewModel by lazy {
        ViewModelProvider(this).get(WeatherViewModel::class.java)
    }

    private var _binding: ActivityWeatherBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val decorView = window.decorView
        decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT

        _binding = ActivityWeatherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 天气内容区域适配系统栏
        ViewCompat.setOnApplyWindowInsetsListener(binding.weatherLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 侧边栏：点击导航按钮打开
        binding.nowItem.navBtn.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        // 侧边栏关闭时收起键盘
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: View) {
                val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                manager.hideSoftInputFromWindow(drawerView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        })

        // 读取 Intent 传入的位置信息
        if(viewModel.locationLng.isEmpty()) {
            viewModel.locationLng = intent.getStringExtra("location_lng") ?: ""
        }
        if(viewModel.locationLat.isEmpty()) {
            viewModel.locationLat = intent.getStringExtra("location_lat") ?: ""
        }
        if(viewModel.placeName.isEmpty()) {
            viewModel.placeName = intent.getStringExtra("place_name") ?: ""
        }

        // 立即显示界面，消除空白等待
        binding.weatherLayout.visibility = View.VISIBLE
        binding.nowItem.placeName.text = viewModel.placeName
        binding.nowItem.root.setBackgroundResource(R.drawable.bg_clear_day)

        // 下拉刷新
        binding.swipeRefresh.setColorSchemeResources(R.color.ic_launcher_background)
        binding.swipeRefresh.setOnRefreshListener {
            refreshWeather()
        }

        // 观察天气数据
        viewModel.weatherLiveData.observe(this, Observer { result ->
            val weather = result.getOrNull()
            if(weather != null) {
                showWeatherInfo(weather)
            }
            else {
                Toast.makeText(this, "无法成功获取天气信息", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
            binding.swipeRefresh.isRefreshing = false
        })

        refreshWeather()
    }

    private fun showWeatherInfo(weather: Weather) {
        binding.nowItem.placeName.text = viewModel.placeName
        val realtime = weather.realtime
        val daily = weather.daily
        // 填充实时天气数据
        val currentTempText = "${realtime.temperature.toInt()} ℃"
        binding.nowItem.currentTemp.text = currentTempText
        binding.nowItem.currentSky.text = getSky(realtime.skycon).info
        val currentPM25Text = "空气指数 ${realtime.airQuality.aqi.chn.toInt()}"
        binding.nowItem.currentAQI.text = currentPM25Text
        binding.nowItem.root.setBackgroundResource(getSky(realtime.skycon).bg)
        // 填充天气预报数据
        binding.forecastItem.forecastLayout.removeAllViews()
        val days = daily.skycon.size
        for(i in 0 until days) {
            val skycon = daily.skycon[i]
            val temperature = daily.temperature[i]
            val view = LayoutInflater.from(this).inflate(R.layout.forecast_item,
                binding.forecastItem.forecastLayout, false)
            val dateInfo = view.findViewById<TextView>(R.id.dateInfo) as TextView
            val skyIcon = view.findViewById<ImageView>(R.id.skyIcon) as ImageView
            val skyInfo = view.findViewById<TextView>(R.id.skyInfo) as TextView
            val temperatureInfo = view.findViewById<TextView>(R.id.temperatureInfo) as TextView
            dateInfo.text = skycon.date.substring(0, 10)
            val sky = getSky(skycon.value)
            skyIcon.setImageResource(sky.icon)
            skyInfo.text = sky.info
            val tempText = "${temperature.min.toInt()} ~ ${temperature.max.toInt()}"
            temperatureInfo.text = tempText
            binding.forecastItem.forecastLayout.addView(view)
        }
        // 填充生活指数数据
        val lifeIndex = daily.lifeIndex
        binding.lifeIndexItem.coldRiskText.text = lifeIndex.coldRisk[0].desc
        binding.lifeIndexItem.dressingText.text = lifeIndex.dressing[0].desc
        binding.lifeIndexItem.ultravioletText.text = lifeIndex.ultraviolet[0].desc
        binding.lifeIndexItem.carWashingText.text = lifeIndex.carWashing[0].desc
    }

    fun refreshWeather() {
        viewModel.refreshWeather(viewModel.locationLng, viewModel.locationLat)
        binding.swipeRefresh.isRefreshing = true
    }
}