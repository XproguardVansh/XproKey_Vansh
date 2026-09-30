package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.remote.dto.payment.CancelSubscriptionResponseDto
import com.xprokeey2.data.remote.dto.payment.CreateSubscriptionResponseDto
import com.xprokeey2.data.remote.dto.payment.StartTrialResponseDto
import com.xprokeey2.data.remote.dto.payment.TrialStatusDto
import com.xprokeey2.domain.util.Resource

interface PaymentRemoteDataSource {
    suspend fun getTrialStatus(): Resource<TrialStatusDto>
    suspend fun startTrial(plan: String): Resource<StartTrialResponseDto>
    suspend fun createSubscription(plan: String): Resource<CreateSubscriptionResponseDto>
    suspend fun cancelSubscription(): Resource<CancelSubscriptionResponseDto>
}
