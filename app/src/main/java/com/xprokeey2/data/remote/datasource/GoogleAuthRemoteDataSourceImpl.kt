package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.remote.api.GoogleAuthApi
import com.xprokeey2.data.remote.dto.auth.GoogleAuthRequestDto
import com.xprokeey2.data.remote.dto.auth.GoogleAuthResponseDto
import com.xprokeey2.data.remote.dto.auth.SetupVaultPasswordRequestDto
import com.xprokeey2.data.remote.util.safeApiCall
import com.xprokeey2.domain.util.Resource
import kotlinx.serialization.json.Json
import javax.inject.Inject

class GoogleAuthRemoteDataSourceImpl @Inject constructor(
    private val api: GoogleAuthApi,
    private val json: Json,
) : GoogleAuthRemoteDataSource {

    override suspend fun signIn(idToken: String): Resource<GoogleAuthResponseDto> =
        safeApiCall(json) { api.signIn(GoogleAuthRequestDto(idToken)) }

    override suspend fun setupVaultPassword(
        setupToken: String,
        request: SetupVaultPasswordRequestDto,
    ): Resource<GoogleAuthResponseDto> =
        safeApiCall(json) { api.setupVaultPassword(authorization = "Bearer $setupToken", body = request) }
}
