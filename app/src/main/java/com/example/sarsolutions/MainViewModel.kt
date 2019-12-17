package com.example.sarsolutions

import androidx.lifecycle.ViewModel
import com.example.sarsolutions.services.LocationService
import com.google.firebase.firestore.GeoPoint

class MainViewModel : ViewModel() {
    var isTestingEnabled = false
    lateinit var lastUpdatedText : String
}
