package com.sarcoordinator.sarsolutions.api

import com.sarcoordinator.sarsolutions.models.*
import retrofit2.Response
import retrofit2.http.*

@JvmSuppressWildcards
interface API {
    @GET("getCases")
    suspend fun getCases(@Header("authorization") tokenId: String): List<Case>

    @GET("getCaseData")
    suspend fun getCaseData(
        @Query("caseId") caseId: String,
        @Header("authorization") tokenId: String
    ): Case

    @POST("postStartShift")
    suspend fun postStartShift(
        @Header("authorization") tokenId: String,
        @Body shift: Shift,
        @Query("caseId") caseId: String,
        @Query("isTest") isTest: Boolean
    ): ShiftId

    // NOTE: Return type 'Response<Unit>' is needed as success http code returned here is 204
    // Kotlin throws a null pointer exception (as of Retrofit 2.7.0)
    @PUT("putLocations")
    suspend fun putLocations(
        @Header("authorization") tokenId: String,
        @Query("shiftId") shiftId: String,
        @Query("isTest") isTest: Boolean,
        @Body paths: LocationBody
    ): Response<Unit>

    @PUT("putEndTime")
    suspend fun putEndTime(
        @Header("authorization") tokenId: String,
        @Query("shiftId") shiftId: String,
        @Query("isTest") isTest: Boolean,
        @Body endTime: EndTimeBody
    ): Response<Unit>

    @POST("postShiftReport")
    suspend fun postShiftReport(
        @Header("authorization") tokenId: String,
        @Query("shiftId") shiftId: String,
        @Body report: ShiftReport
    ): Response<Unit>

    @GET("getUser")
    suspend fun getUser(
        @Header("authorization") tokenId: String,
        @Query("uid") userId: String
    ): Volunteer
}