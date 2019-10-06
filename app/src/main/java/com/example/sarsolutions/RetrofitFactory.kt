package com.example.sarsolutions

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitFactory {
    private val FIREBASE_URL = "https://us-central1-sar-solutions.cloudfunctions.net/"

    fun makeFirebaseRetrofitService() : IFirebaseService {
        return Retrofit.Builder()
            .baseUrl(FIREBASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build().create(IFirebaseService::class.java)
    }
}