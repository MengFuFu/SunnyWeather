package com.sunnyweather.android.ui.place

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.sunnyweather.android.MainActivity
import com.sunnyweather.android.R
import com.sunnyweather.android.databinding.FragmentPlaceBinding
import com.sunnyweather.android.databinding.FragmentPlaceBinding.bind
import com.sunnyweather.android.ui.weather.WeatherActivity

class PlaceFragment : Fragment() {
    val viewModel by lazy {
        ViewModelProvider(this).get(PlaceViewModel::class.java)
    }

    private lateinit var adapter : PlaceAdapter

    // ViewBinding 标准写法
    private var _binding: FragmentPlaceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        //初始化视图
        val layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.layoutManager = layoutManager
        adapter = PlaceAdapter(this, viewModel.placeList)
        binding.recyclerView.adapter = adapter

        binding.searchPlaceEdit.addTextChangedListener {
                editable ->
            val content = editable.toString()
            if(content.isNotEmpty()) {
                viewModel.searchPlaces(content)
            }
            else {
                binding.recyclerView.visibility = View.GONE
                binding.bgImageView.visibility = View.VISIBLE
                viewModel.placeList.clear()
                adapter.notifyDataSetChanged()
            }
        }

        //观察手动搜索结果
        viewModel.placeLiveData.observe(viewLifecycleOwner, Observer {
                result ->
            val places = result.getOrNull()
            if(places != null) {
                binding.recyclerView.visibility = View.VISIBLE
                binding.bgImageView.visibility = View.GONE
                viewModel.placeList.clear()
                viewModel.placeList.addAll(places)
                adapter.notifyDataSetChanged()
            }
            else {
                Toast.makeText(activity, "未能查询到任何地点", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
        })

        //观察自动定位结果
        viewModel.autoLocateResult.observe(viewLifecycleOwner, Observer {
            result ->
            val place = result.getOrNull()
            if(place != null && activity is MainActivity) {
                //保存并跳转天气界面
                viewModel.savePlace(place)
                val intent = Intent(context, WeatherActivity::class.java).apply {
                    putExtra("location_lng", place.location.lng)
                    putExtra("location_lat", place.location.lat)
                    putExtra("place_name", place.name)
                }
                startActivity(intent)
                activity?.finish()
                return@Observer
            }

            // 自动定位失败（或不在 MainActivity）：降级处理
            if(activity is MainActivity) {
                // 只有在 MainActivity 中才执行降级（避免在 WeatherActivity 中干扰）
                if(viewModel.isPlaceSaved()) {
                    //有保存的城市直接跳转
                    val savedPlace = viewModel.getSavedPlace()
                    val intent = Intent(context, WeatherActivity::class.java).apply {
                        putExtra("location_lng", savedPlace.location.lng)
                        putExtra("location_lat", savedPlace.location.lat)
                        putExtra("place_name", savedPlace.name)
                    }
                    startActivity(intent)
                    activity?.finish()
                    return@Observer
                }
                else {
                    // 无保存城市且定位失败：停留在当前页面，显示具体错误
                    val errorMsg = result.exceptionOrNull()?.message ?: "未知错误"
                    Toast.makeText(activity, "自动定位失败：$errorMsg", Toast.LENGTH_LONG).show()
                }
            }
        })

        //触发自动定位（需检查定位权限），仅在 MainActivity 中
        if(activity is MainActivity) {
            if (hasLocationPermission()) {
                viewModel.autoLocate()
            } else {
                requestLocationPermission()
            }
        }

    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    // 权限已授予，触发定位
                    viewModel.autoLocate()
                } else {
                    // 权限被拒绝，走降级逻辑
                    handlePermissionDenied()
                }
            }
        }
    }

    private fun handlePermissionDenied() {
        if (activity is MainActivity) {
            if (viewModel.isPlaceSaved()) {
                val savedPlace = viewModel.getSavedPlace()
                val intent = Intent(context, WeatherActivity::class.java).apply {
                    putExtra("location_lng", savedPlace.location.lng)
                    putExtra("location_lat", savedPlace.location.lat)
                    putExtra("place_name", savedPlace.name)
                }
                startActivity(intent)
                activity?.finish()
            } else {
                Toast.makeText(
                    activity, "定位权限未授予，请手动搜索城市", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}