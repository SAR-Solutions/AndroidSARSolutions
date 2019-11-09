package com.example.sarsolutions

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.GeoPoint

class MainActivityViewModel : ViewModel(){
    private val locationList: ArrayList<GeoPoint> = ArrayList()
    var isSearching = false
    var isTestingEnabled = false
    private var lastUpdated: String? = null

    fun getLocationList(): ArrayList<GeoPoint> {
        return locationList
    }

    // Return true if newPoint exists in list, else false
    fun existsInList(newPoint: GeoPoint): Boolean {
        locationList.forEach { point ->
            if (point.compareTo(newPoint) == 0)
                return true
        }
        return false
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