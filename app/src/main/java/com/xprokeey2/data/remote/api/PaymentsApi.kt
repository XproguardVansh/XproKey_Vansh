package com.xprokeey2.data.remote.api

import com.xprokeey2.data.remote.dto.payment.CancelSubscriptionResponseDto
import com.xprokeey2.data.remote.dto.payment.CreateSubscriptionResponseDto
import com.xprokeey2.data.remote.dto.payment.PlanRequestDto
import com.xprokeey2.data.remote.dto.payment.StartTrialResponseDto
import com.xprokeey2.data.remote.dto.payment.TrialStatusDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/** Personal subscriptions (Razorpay). The server's access guard skips these, so a blocked user can still pay. */
interface PaymentsApi {

    @GET("payments/trial-status")
    suspend fun getTrialStatus(@Header("Authorization") authorization: String): TrialStatusDto

    @POST("payments/start-yearly-trial")
    suspend fun startTrial(
        @Header("Authorization") authorization: String,
        @Body body: PlanRequestDto,
    ): StartTrialResponseDto

    @POST("payments/create-subscription")
    suspend fun createSubscription(
        @Header("Authorization") authorization: String,
        @Body body: PlanRequestDto,
    ): CreateSubscriptionResponseDto

    @POST("payments/cancel-subscription")
    suspend fun cancelSubscription(@Header("Authorization") authorization: String): CancelSubscriptionResponseDto
}
