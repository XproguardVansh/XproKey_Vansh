package com.xprokeey2.domain.repository

/** Vault material of the saved login session. */
interface SessionRepository {

    /** The account's master salt saved at login; null when nobody is signed in. */
    suspend fun getMasterSalt(): String?

    /** Keeps the saved encrypted vault key current after the password changed. */
    suspend fun saveEncryptedVaultKey(encryptedVaultKey: String)
}
