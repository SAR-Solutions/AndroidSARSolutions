package com.example.sarsolutions

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface IFirebaseService {

    @POST("postShift")
    fun createShift(@Body shift: Shift) : Call<String>

//    @POST("putLocations")
//    fun createLocations() : Call<>
}