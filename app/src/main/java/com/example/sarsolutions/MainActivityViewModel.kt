package com.example.sarsolutions

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.GeoPoint

class MainActivityViewModel : ViewModel(){
    private val locationList : ArrayList<GeoPoint> = ArrayList<GeoPoint>()

    fun getLocationList(): ArrayList<GeoPoint> {
        return locationList
    }

    fun addToList(geoPoint: GeoPoint) {
        locationList.add(geoPoint)
    }

    fun clearList() {
        locationList.clear()
    }

}