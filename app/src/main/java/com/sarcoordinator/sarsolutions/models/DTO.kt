package com.sarcoordinator.sarsolutions.models

import com.google.firebase.firestore.GeoPoint

data class Shift(
    val caseId: String,
    val userId: String,
    val startTime: String,
    val endTime: String,
    val app_version: String,
    val path: List<GeoPoint>
)

data class Cases(val caseIds: List<String>)

data class Case(
    val description: String,
    val equipmentUsed: List<String>,
    val missingPersonName: List<String>,
    val reporterName: String,
    val volunteers: List<Volunteer>,
    val date: Long
)

data class Volunteer(
    val badeNumber: String,
    val roles: List<String>,
    val name: String,
    val userId: String
)
