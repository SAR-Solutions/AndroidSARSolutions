package com.example.sarsolutions

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.GeoPoint

class MainActivityViewModel : ViewModel(){
    private val locationList : ArrayList<GeoPoint> = ArrayList<GeoPoint>()
    public var isSearching = false
    private var lastUpdated: String? = null

    fun getLocationList(): ArrayList<GeoPoint> {
        return locationList
    }

    fun addToList(geoPoint: GeoPoint) {
        locationList.add(geoPoint)
    }

    // Resets instance variables to default values
    fun endShift() {
        isSearching = false
        lastUpdated = null
        locationList.clear()
    }

    fun getLastUpdated(): String? {
        return lastUpdated
    }

    fun setLastUpdated(time: String) {
        lastUpdated = time
    }

}