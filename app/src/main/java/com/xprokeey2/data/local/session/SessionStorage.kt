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
) {

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

    suspend fun getAccessToken(): String? = readSecret(ACCESS_TOKEN)

    suspend fun getRefreshToken(): String? = readSecret(REFRESH_TOKEN)

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
