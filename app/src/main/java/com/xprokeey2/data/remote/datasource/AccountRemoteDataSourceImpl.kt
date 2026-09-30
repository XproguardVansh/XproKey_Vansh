package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.local.session.AccessTokenStore
import com.xprokeey2.data.remote.api.AccountApi
import com.xprokeey2.data.remote.dto.account.BillingDto
import com.xprokeey2.data.remote.dto.account.DashboardSummaryDto
import com.xprokeey2.data.remote.dto.account.MeDto
import com.xprokeey2.data.remote.dto.account.SecuritySettingsDto
import com.xprokeey2.data.remote.util.authorizedApiCall
import com.xprokeey2.domain.util.Resource
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AccountRemoteDataSourceImpl @Inject constructor(
    private val api: AccountApi,
    private val tokenStore: AccessTokenStore,
    private val json: Json,
) : AccountRemoteDataSource {

    override suspend fun getMe(): Resource<MeDto> =
        authorizedApiCall(tokenStore, json) { api.getMe(it) }

    override suspend fun logout(): Resource<Unit> =
        authorizedApiCall(tokenStore, json) { api.logout(it) }

    override suspend fun getSecuritySettings(): Resource<SecuritySettingsDto> =
        authorizedApiCall(tokenStore, json) { api.getSecuritySettings(it) }

    override suspend fun updateSecuritySettings(request: SecuritySettingsDto): Resource<Unit> =
        authorizedApiCall(tokenStore, json) { api.updateSecuritySettings(it, request) }

    override suspend fun getBilling(): Resource<BillingDto> =
        authorizedApiCall(tokenStore, json) { api.getBilling(it) }

    override suspend fun getDashboardSummary(): Resource<DashboardSummaryDto> =
        authorizedApiCall(tokenStore, json) { api.getDashboardSummary(it) }
}
