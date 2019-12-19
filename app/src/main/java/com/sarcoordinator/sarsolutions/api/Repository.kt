package com.sarcoordinator.sarsolutions.api

import com.sarcoordinator.sarsolutions.models.Case
import com.sarcoordinator.sarsolutions.models.Cases
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object Repository {
    private const val SAR_FUNCTIONS_URL = "https://us-central1-sar-solutions.cloudfunctions.net/"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(45, TimeUnit.SECONDS)
            .connectTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    private val service: API by lazy {
        Retrofit.Builder().baseUrl(SAR_FUNCTIONS_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build().create(API::class.java)
    }

    suspend fun getCases(): Cases {
        return service.getCases()
    }

    suspend fun getCaseDetail(id: String): Case {
        return service.getCaseData(id)
    }
}