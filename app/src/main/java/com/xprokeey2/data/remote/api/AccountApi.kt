package com.xprokeey2.data.remote.api

import com.xprokeey2.data.remote.dto.account.BillingDto
import com.xprokeey2.data.remote.dto.account.DashboardSummaryDto
import com.xprokeey2.data.remote.dto.account.MeDto
import com.xprokeey2.data.remote.dto.account.SecuritySettingsDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

/** The signed-in account: profile, session-timeout settings, billing, the summary, and logging out. */
interface AccountApi {

    @GET("me")
    suspend fun getMe(@Header("Authorization") authorization: String): MeDto

    /** Ends the session on the server; the web sends no body. */
    @POST("logout")
    suspend fun logout(@Header("Authorization") authorization: String)

    @GET("me/security")
    suspend fun getSecuritySettings(@Header("Authorization") authorization: String): SecuritySettingsDto

    @PUT("me/security")
    suspend fun updateSecuritySettings(
        @Header("Authorization") authorization: String,
        @Body body: SecuritySettingsDto,
    )

    @GET("me/billing")
    suspend fun getBilling(@Header("Authorization") authorization: String): BillingDto

    @GET("dashboard/summary")
    suspend fun getDashboardSummary(@Header("Authorization") authorization: String): DashboardSummaryDto
}
