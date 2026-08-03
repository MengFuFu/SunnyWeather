package com.sunnyweather.android.ui.place

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.sunnyweather.android.R
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.sunnyweather.android.logic.model.Place
import com.sunnyweather.android.logic.model.Weather
import com.sunnyweather.android.ui.weather.WeatherActivity

class PlaceAdapter(private val fragment: PlaceFragment, private val placeList: List<Place>) :
    RecyclerView.Adapter<PlaceAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val placeName: TextView = view.findViewById<TextView>(R.id.placeName)
        val placeAddress: TextView = view.findViewById<TextView>(R.id.placeAddress)
    }

    override fun onCreateViewHolder(
        p0: ViewGroup,
        p1: Int
    ): PlaceAdapter.ViewHolder {
        val view = LayoutInflater.from(p0.context).inflate(R.layout.place_item, p0, false)
        val holder = ViewHolder(view)
        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            val place = placeList[position]
            val intent = Intent(p0.context, WeatherActivity::class.java).apply {
                putExtra("location_lng", place.location.lng)
                putExtra("location_lat", place.location.lat)
                putExtra("place_name", place.name)
            }
            fragment.viewModel.savePlace(place)
            fragment.startActivity(intent)
            fragment.activity?.finish()
        }
        return holder
    }

    override fun onBindViewHolder(p0: PlaceAdapter.ViewHolder, p1: Int) {
        val place = placeList[p1]
        p0.placeName.text = place.name
        p0.placeAddress.text = place.address
    }

    override fun getItemCount(): Int = placeList.size
}