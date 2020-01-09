package com.sarcoordinator.sarsolutions.api

import com.sarcoordinator.sarsolutions.models.*
import retrofit2.Response
import retrofit2.http.*

@JvmSuppressWildcards
interface API {
    @GET("getCases")
    suspend fun getCases(): Cases

    @GET("getCaseData")
    suspend fun getCaseData(@Query("caseId") caseId: String): Case

    @POST("postStartShiftAuth")
    suspend fun postStartShift(
        @Header("token-id") tokenId: String,
        @Body shift: Shift,
        @Query("caseId") caseId: String,
        @Query("isTest") isTest: Boolean
    ): ShiftId

    // NOTE: Return type 'Response<Unit>' is needed as success http code returned here is 204
    // Kotlin throws a null pointer exception (as of Retrofit 2.7.0)
    @PUT("putLocationsAuth")
    suspend fun putLocationsAuth(
        @Header("token-id") tokenId: String,
        @Query("shiftId") shiftId: String,
        @Query("isTest") isTest: Boolean,
        @Body paths: LocationBody
    ): Response<Unit>
}