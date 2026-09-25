package com.xprokeey2.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xprokeey2.data.local.security.KeystoreCipher
import com.xprokeey2.di.RecoveryKeyDataStore
import com.xprokeey2.domain.repository.RecoveryKeyRepository
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Pending recovery keys, encrypted with the Android Keystore and keyed by email. */
@Singleton
class RecoveryKeyRepositoryImpl @Inject constructor(
    @param:RecoveryKeyDataStore private val dataStore: DataStore<Preferences>,
    private val cipher: KeystoreCipher,
) : RecoveryKeyRepository {

    override suspend fun savePendingKey(email: String, recoveryKey: String) {
        dataStore.edit { it[keyFor(email)] = cipher.encrypt(recoveryKey) }
    }

    override suspend fun getPendingKey(email: String): String? =
        dataStore.data.first()[keyFor(email)]?.let(cipher::decrypt)

    override suspend fun deletePendingKey(email: String) {
        dataStore.edit { it.remove(keyFor(email)) }
    }

    private fun keyFor(email: String) =
        stringPreferencesKey("recovery_key_" + email.trim().lowercase(Locale.ROOT))
}
