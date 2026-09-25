package com.xprokeey2.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.xprokeey2.data.local.security.KeystoreCipher
import com.xprokeey2.data.repository.RecoveryKeyRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on a real Android Keystore: the pieces that make the recovery-key dialog appear. */
@RunWith(AndroidJUnit4::class)
class SecureStorageTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun keystoreCipherRoundTripsAcrossInstances() {
        val encrypted = KeystoreCipher().encrypt("secret-value")
        // A new instance (e.g. after the app restarts) must reuse the same Keystore key.
        assertEquals("secret-value", KeystoreCipher().decrypt(encrypted))
    }

    @Test
    fun pendingRecoveryKeySurvivesUntilDeleted() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("test_pending_recovery_keys")
        }
        val repository = RecoveryKeyRepositoryImpl(dataStore, KeystoreCipher())

        // Saved with the email from the signup response, read back with what the user typed.
        repository.savePendingKey("New.User@Example.com", "nKa9bXmzrOzVIPoW9Y4gBNbvvt8lmKqr+gOBcO4xGos=")
        assertEquals(
            "nKa9bXmzrOzVIPoW9Y4gBNbvvt8lmKqr+gOBcO4xGos=",
            repository.getPendingKey(" new.user@example.com "),
        )

        repository.deletePendingKey("new.user@example.com")
        assertNull(repository.getPendingKey("new.user@example.com"))
    }
}
