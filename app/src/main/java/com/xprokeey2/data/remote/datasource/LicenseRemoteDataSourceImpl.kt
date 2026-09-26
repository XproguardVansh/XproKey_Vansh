package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.remote.api.LicenseApi
import com.xprokeey2.data.remote.dto.license.ActivateLicenseRequestDto
import com.xprokeey2.data.remote.dto.license.ActivateLicenseResponseDto
import com.xprokeey2.data.remote.util.safeApiCall
import com.xprokeey2.domain.util.Resource
import kotlinx.serialization.json.Json
import javax.inject.Inject

class LicenseRemoteDataSourceImpl @Inject constructor(
    private val api: LicenseApi,
    private val json: Json,
) : LicenseRemoteDataSource {

    override suspend fun activateLicense(
        accessToken: String,
        request: ActivateLicenseRequestDto,
    ): Resource<ActivateLicenseResponseDto> =
        safeApiCall(json) { api.activateLicense("Bearer $accessToken", request) }
}
