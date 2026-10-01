package com.xprokeey2.data.repository

import com.xprokeey2.data.local.session.SessionStorage
import com.xprokeey2.data.mapper.toOutcome
import com.xprokeey2.data.mapper.toSessionEntity
import com.xprokeey2.data.mapper.toSessionOrNull
import com.xprokeey2.data.remote.datasource.GoogleAuthRemoteDataSource
import com.xprokeey2.data.remote.dto.auth.SetupVaultPasswordRequestDto
import com.xprokeey2.domain.model.EncryptedVaultKeys
import com.xprokeey2.domain.model.GoogleAuthOutcome
import com.xprokeey2.domain.model.GoogleSession
import com.xprokeey2.domain.repository.GoogleAuthRepository
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

class GoogleAuthRepositoryImpl @Inject constructor(
    private val remote: GoogleAuthRemoteDataSource,
    private val sessionStorage: SessionStorage,
) : GoogleAuthRepository {

    override suspend fun signIn(idToken: String): Resource<GoogleAuthOutcome> =
        when (val result = remote.signIn(idToken)) {
            is Resource.Success -> result.data.toOutcome()?.let { Resource.Success(it) }
                ?: Resource.Error(DataError.Unknown("Unexpected answer from POST /auth/google"))
            is Resource.Error -> result
        }

    override suspend fun setupVaultPassword(
        setupToken: String,
        password: String,
        confirmPassword: String,
        vaultKeys: EncryptedVaultKeys,
    ): Resource<GoogleSession?> {
        val request = SetupVaultPasswordRequestDto(
            password = password,
            confirmPassword = confirmPassword,
            masterSalt = vaultKeys.masterSalt,
            encryptedVaultKey = vaultKeys.encryptedVaultKey,
            encryptedVaultKeyRecovery = vaultKeys.encryptedVaultKeyRecovery,
        )
        return when (val result = remote.setupVaultPassword(setupToken, request)) {
            is Resource.Success -> Resource.Success(result.data.toSessionOrNull())
            is Resource.Error -> result
        }
    }

    override suspend fun saveSession(session: GoogleSession) {
        sessionStorage.save(session.toSessionEntity())
    }
}
