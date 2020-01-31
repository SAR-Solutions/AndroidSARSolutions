package com.sarcoordinator.sarsolutions

import androidx.lifecycle.ViewModel

class ShiftReportViewModel : ViewModel() {
    var vehicleList = ArrayList<VehicleCardContent>()

    fun addVehicle() {
        vehicleList.add(
            VehicleCardContent(
                name = "Vehicle ${vehicleList.size + 1}",
                isCountyVehicle = false,
                isPersonalVehicle = false,
                milesTraveled = null,
                vehicleType = 0
            )
        )
    }

    fun updateAtPosition(position: Int, vehicle: VehicleCardContent) {
        vehicleList[position] = vehicle
    }

    data class VehicleCardContent(
        var name: String,
        var isCountyVehicle: Boolean,
        var isPersonalVehicle: Boolean,
        var milesTraveled: String?,
        var vehicleType: Int
    )
}