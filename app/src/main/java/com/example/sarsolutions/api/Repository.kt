package com.example.sarsolutions.api

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object Repository {
    private const val SAR_FUNCTIONS_URL = "https://us-central1-sar-solutions.cloudfunctions.net/"
    private val service: API by lazy {
        Retrofit.Builder().baseUrl(SAR_FUNCTIONS_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build().create(API::class.java)
    }

    suspend fun getCases() = service.getCases()
    suspend fun getCaseDetail(id: String) = service.getCaseData(id)
}