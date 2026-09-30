package com.xprokeey2.data.local.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xprokeey2.data.local.security.KeystoreCipher
import com.xprokeey2.di.SessionDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the login session. Secrets are encrypted with [KeystoreCipher] before hitting disk. */
@Singleton
class SessionStorage @Inject constructor(
    @param:SessionDataStore private val dataStore: DataStore<Preferences>,
    private val cipher: KeystoreCipher,
) : AccessTokenStore {

    suspend fun save(session: SessionEntity) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = cipher.encrypt(session.accessToken)
            prefs[REFRESH_TOKEN] = cipher.encrypt(session.refreshToken)
            prefs[ENCRYPTED_VAULT_KEY] = cipher.encrypt(session.encryptedVaultKey)
            prefs[MASTER_SALT] = cipher.encrypt(session.masterSalt)
            prefs[USER_ID] = session.userId
            prefs[NAME] = session.name
            prefs[EMAIL] = session.email
        }
    }

    override suspend fun getAccessToken(): String? = readSecret(ACCESS_TOKEN)

    override suspend fun getRefreshToken(): String? = readSecret(REFRESH_TOKEN)

    /** After POST /refresh: only the access token changes (the refresh token isn't rotated). */
    override suspend fun saveAccessToken(accessToken: String) {
        dataStore.edit { it[ACCESS_TOKEN] = cipher.encrypt(accessToken) }
    }

    suspend fun getUserId(): String? = dataStore.data.first()[USER_ID]

    /** The account's master salt from login (the web keeps it in sessionStorage). */
    suspend fun getMasterSalt(): String? = readSecret(MASTER_SALT)

    /** The vault key locked with the master password, for the Lock screen (the web keeps it in localStorage). */
    suspend fun getEncryptedVaultKey(): String? = readSecret(ENCRYPTED_VAULT_KEY)

    /** After Settings > Change password: the vault key as now locked with the new password. */
    suspend fun saveEncryptedVaultKey(encryptedVaultKey: String) {
        dataStore.edit { it[ENCRYPTED_VAULT_KEY] = cipher.encrypt(encryptedVaultKey) }
    }

    /** Who is signed in, or null when no session is saved. */
    suspend fun getProfile(): SessionProfile? {
        val prefs = dataStore.data.first()
        val userId = prefs[USER_ID] ?: return null
        return SessionProfile(userId = userId, name = prefs[NAME].orEmpty(), email = prefs[EMAIL].orEmpty())
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private suspend fun readSecret(key: Preferences.Key<String>): String? =
        dataStore.data.first()[key]?.let(cipher::decrypt)

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val ENCRYPTED_VAULT_KEY = stringPreferencesKey("encrypted_vault_key")
        val MASTER_SALT = stringPreferencesKey("master_salt")
        val USER_ID = stringPreferencesKey("user_id")
        val NAME = stringPreferencesKey("name")
        val EMAIL = stringPreferencesKey("email")
    }
}
