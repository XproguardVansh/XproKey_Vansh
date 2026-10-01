package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.remote.dto.auth.GoogleAuthResponseDto
import com.xprokeey2.data.remote.dto.auth.SetupVaultPasswordRequestDto
import com.xprokeey2.domain.util.Resource

interface GoogleAuthRemoteDataSource {
    suspend fun signIn(idToken: String): Resource<GoogleAuthResponseDto>
    suspend fun setupVaultPassword(setupToken: String, request: SetupVaultPasswordRequestDto): Resource<GoogleAuthResponseDto>
}
