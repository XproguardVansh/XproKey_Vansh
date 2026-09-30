package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.local.session.AccessTokenStore
import com.xprokeey2.data.remote.api.PaymentsApi
import com.xprokeey2.data.remote.dto.payment.CancelSubscriptionResponseDto
import com.xprokeey2.data.remote.dto.payment.CreateSubscriptionResponseDto
import com.xprokeey2.data.remote.dto.payment.PlanRequestDto
import com.xprokeey2.data.remote.dto.payment.StartTrialResponseDto
import com.xprokeey2.data.remote.dto.payment.TrialStatusDto
import com.xprokeey2.data.remote.util.authorizedApiCall
import com.xprokeey2.domain.util.Resource
import kotlinx.serialization.json.Json
import javax.inject.Inject

class PaymentRemoteDataSourceImpl @Inject constructor(
    private val api: PaymentsApi,
    private val tokenStore: AccessTokenStore,
    private val json: Json,
) : PaymentRemoteDataSource {

    override suspend fun getTrialStatus(): Resource<TrialStatusDto> =
        authorizedApiCall(tokenStore, json) { api.getTrialStatus(it) }

    override suspend fun startTrial(plan: String): Resource<StartTrialResponseDto> =
        authorizedApiCall(tokenStore, json) { api.startTrial(it, PlanRequestDto(plan)) }

    override suspend fun createSubscription(plan: String): Resource<CreateSubscriptionResponseDto> =
        authorizedApiCall(tokenStore, json) { api.createSubscription(it, PlanRequestDto(plan)) }

    override suspend fun cancelSubscription(): Resource<CancelSubscriptionResponseDto> =
        authorizedApiCall(tokenStore, json) { api.cancelSubscription(it) }
}
