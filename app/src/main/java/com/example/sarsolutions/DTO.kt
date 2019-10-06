package com.example.sarsolutions

import com.google.firebase.firestore.GeoPoint
import java.util.*

data class Shift(val caseId: String, val userId: String, val startTime: String, val path: List<GeoPoint>)