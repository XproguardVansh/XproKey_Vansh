package com.xprokeey2.data.repository

import com.xprokeey2.data.local.session.SessionStorage
import com.xprokeey2.domain.repository.SessionRepository
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionStorage: SessionStorage,
) : SessionRepository {

    override suspend fun getMasterSalt(): String? = sessionStorage.getMasterSalt()

    override suspend fun getEncryptedVaultKey(): String? = sessionStorage.getEncryptedVaultKey()

    override suspend fun saveEncryptedVaultKey(encryptedVaultKey: String) =
        sessionStorage.saveEncryptedVaultKey(encryptedVaultKey)
}
