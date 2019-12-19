package com.sarcoordinator.sarsolutions.api

import com.sarcoordinator.sarsolutions.models.Case
import com.sarcoordinator.sarsolutions.models.Cases
import retrofit2.http.GET
import retrofit2.http.Query

interface API {
    @GET("getCases")
    suspend fun getCases(): Cases

    @GET("getCaseData")
    suspend fun getCaseData(@Query("caseId") caseId: String): Case
}