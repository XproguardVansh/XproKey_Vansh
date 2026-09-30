package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.remote.dto.account.BillingDto
import com.xprokeey2.data.remote.dto.account.DashboardSummaryDto
import com.xprokeey2.data.remote.dto.account.MeDto
import com.xprokeey2.data.remote.dto.account.SecuritySettingsDto
import com.xprokeey2.domain.util.Resource

interface AccountRemoteDataSource {
    suspend fun getMe(): Resource<MeDto>
    suspend fun logout(): Resource<Unit>
    suspend fun getSecuritySettings(): Resource<SecuritySettingsDto>
    suspend fun updateSecuritySettings(request: SecuritySettingsDto): Resource<Unit>
    suspend fun getBilling(): Resource<BillingDto>
    suspend fun getDashboardSummary(): Resource<DashboardSummaryDto>
}
