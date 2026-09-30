package com.xprokeey2.domain.repository

/** Vault material of the saved login session. */
interface SessionRepository {

    /** Someone is signed in on this device: the login tokens are saved (logging out removes them). */
    suspend fun hasSession(): Boolean

    /** The account's master salt saved at login; null when nobody is signed in. */
    suspend fun getMasterSalt(): String?

    /** The vault key locked with the master password, saved at login; null when nobody is signed in. */
    suspend fun getEncryptedVaultKey(): String?

    /** Keeps the saved encrypted vault key current after the password changed. */
    suspend fun saveEncryptedVaultKey(encryptedVaultKey: String)
}
