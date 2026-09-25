package com.xprokeey2.domain.repository

/**
 * Recovery keys generated at signup, kept (encrypted) on this device until the
 * account is verified and the user has seen the "Save your recovery key" dialog.
 */
interface RecoveryKeyRepository {
    suspend fun savePendingKey(email: String, recoveryKey: String)
    suspend fun getPendingKey(email: String): String?
    suspend fun deletePendingKey(email: String)
}
