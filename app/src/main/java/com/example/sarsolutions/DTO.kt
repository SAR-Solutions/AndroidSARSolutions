package com.example.sarsolutions

import java.util.*

data class GeoLocation(val lat: String, val long: String)

data class Shift(val CaseID: String, val UserID: String, val StartTime: String, val locations: List<GeoLocation>)