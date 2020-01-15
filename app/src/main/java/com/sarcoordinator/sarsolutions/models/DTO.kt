package com.sarcoordinator.sarsolutions.models

import java.io.Serializable

data class ShiftId(val shiftId: String)

data class Shift(
    val startTime: String,
    val appVersion: String
)

data class Cases(val caseIds: List<String>)

data class LocationPoint(val latitude: Double, val longitude: Double)

data class LocationBody(val paths: List<LocationPoint>)

data class EndTimeBody(val endTime: String)

data class Case(
    var id: String,
    val description: String,
    val equipmentUsed: List<String>,
    val missingPersonName: List<String>,
    val reporterName: String,
    val volunteers: List<Volunteer>,
    val date: Long
) : Serializable

data class Volunteer(
    val badeNumber: String,
    val roles: List<String>,
    val name: String,
    val userId: String
) : Serializable
