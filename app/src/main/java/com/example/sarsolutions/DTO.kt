package com.example.sarsolutions

import com.google.firebase.firestore.GeoPoint

data class Shift(
    val caseId: String,
    val userId: String,
    val startTime: String,
    val app_version: String,
    val path: List<GeoPoint>
)