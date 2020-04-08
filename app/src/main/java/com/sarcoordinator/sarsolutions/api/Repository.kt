package com.sarcoordinator.sarsolutions.api

import android.location.Location
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.sarcoordinator.sarsolutions.models.*
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object Repository {
    private const val SAR_FUNCTIONS_URL = "https://us-central1-sar-solutions.cloudfunctions.net/"

    private val user: FirebaseUser = FirebaseAuth.getInstance().currentUser!!

    private var tlsSpecs = listOf(ConnectionSpec.MODERN_TLS)
    private val interceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val client: OkHttpClient by lazy {
        /* providing backwards-compatibility for API lower than Lollipop: */
        OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .connectionSpecs(tlsSpecs)
            .build()
    }

    private val service: API by lazy {
        Retrofit.Builder().baseUrl(SAR_FUNCTIONS_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build().create(API::class.java)
    }

    suspend fun getCases(): List<Case> =
        service.getCases(getToken())


    suspend fun getCaseDetail(caseId: String): Case =
        service.getCaseData(caseId, getToken())

    suspend fun postStartShift(
        shift: Shift,
        caseId: String,
        isTest: Boolean
    ) =
        service.postStartShift(getToken(), shift, caseId, isTest)

    suspend fun putLocations(
        shiftId: String,
        isTest: Boolean,
        paths: List<Location>
    ) {
        val parsedLocList = ArrayList<LocationPoint>()
        paths.forEach {
            parsedLocList.add(LocationPoint(it.latitude, it.longitude))
        }
        service.putLocations(getToken(), shiftId, isTest, LocationBody(parsedLocList))
    }

    suspend fun putLocationPoints(
        shiftId: String,
        isTest: Boolean,
        paths: List<LocationPoint>
    ) = service.putLocations(getToken(), shiftId, isTest, LocationBody(paths))


    suspend fun putEndTime(
        shiftId: String,
        isTest: Boolean,
        endTime: String
    ) =
        service.putEndTime(getToken(), shiftId, isTest, EndTimeBody(endTime))

    suspend fun postShiftReport(
        shiftId: String,
        report: ShiftReport
    ) =
        service.postShiftReport(getToken(), shiftId, report)

    // Synchronously get user token
    private fun getToken(): String {
        return Tasks.await(user.getIdToken(true)).token!!
    }
}