package com.xprokeey2.data.remote.api

import com.xprokeey2.data.remote.dto.account.BillingDto
import com.xprokeey2.data.remote.dto.account.DashboardSummaryDto
import com.xprokeey2.data.remote.dto.account.SecuritySettingsDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT

/** The signed-in account: session-timeout settings, billing and the summary the Subscription page shows. */
interface AccountApi {

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
