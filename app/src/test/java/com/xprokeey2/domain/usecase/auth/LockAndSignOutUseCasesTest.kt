package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.data.crypto.VaultCryptoImpl
import com.xprokeey2.data.local.session.InMemoryVaultSession
import com.xprokeey2.domain.model.CancelledSubscription
import com.xprokeey2.domain.model.EncryptedVaultKeys
import com.xprokeey2.domain.model.ForgotPasswordResult
import com.xprokeey2.domain.model.LoginResult
import com.xprokeey2.domain.model.SignupResult
import com.xprokeey2.domain.model.SubscriptionPlan
import com.xprokeey2.domain.repository.AuthRepository
import com.xprokeey2.domain.repository.SessionRepository
import com.xprokeey2.domain.repository.SubscriptionRepository
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The session timeout's Lock and Log out, and the Lock screen's unlock. */
class LockAndSignOutUseCasesTest {

    private class FakeSessionRepository(
        private val encryptedVaultKey: String?,
        private val masterSalt: String?,
    ) : SessionRepository {
        override suspend fun hasSession() = true
        override suspend fun getMasterSalt() = masterSalt
        override suspend fun getEncryptedVaultKey() = encryptedVaultKey
        override suspend fun saveEncryptedVaultKey(encryptedVaultKey: String) = error("unused")
    }

    private val crypto = VaultCryptoImpl()

    @Test
    fun theMasterPasswordUnlocksTheSavedVaultKey() = runBlocking {
        val vault = crypto.createVault("Master-Password1")
        val keys = vault.encryptedKeys
        val expected = crypto.unlockVaultKey("Master-Password1", keys.masterSalt, keys.encryptedVaultKey)
        val vaultSession = InMemoryVaultSession()
        val unlock = UnlockVaultUseCase(FakeSessionRepository(keys.encryptedVaultKey, keys.masterSalt), crypto, vaultSession)

        assertEquals(UnlockVaultResult.WRONG_PASSWORD, unlock("wrong-password"))
        assertNull(vaultSession.vaultKey)

        assertEquals(UnlockVaultResult.UNLOCKED, unlock("Master-Password1"))
        assertEquals(expected, vaultSession.vaultKey)
    }

    @Test
    fun withoutSavedKeysThereIsNothingToUnlock() = runBlocking {
        for (repository in listOf(FakeSessionRepository(null, "c2FsdA=="), FakeSessionRepository("a2V5", ""), FakeSessionRepository(null, null))) {
            assertEquals(UnlockVaultResult.SESSION_DATA_MISSING, UnlockVaultUseCase(repository, crypto, InMemoryVaultSession())("anything"))
        }
    }

    @Test
    fun lockingOnlyForgetsTheVaultKey() {
        val vaultSession = InMemoryVaultSession().apply { unlock("vault-key") }

        LockVaultUseCase(vaultSession)()

        assertNull(vaultSession.vaultKey)
    }

    @Test
    fun signingOutClearsTheSessionAndAPendingPayment() = runBlocking {
        var sessionCleared = false
        val auth = object : AuthRepository {
            override suspend fun clearSession() {
                sessionCleared = true
            }
            override suspend fun signup(name: String, email: String, password: String, confirmPassword: String, vaultKeys: EncryptedVaultKeys): Resource<SignupResult> = error("unused")
            override suspend fun login(email: String, password: String): Resource<LoginResult> = error("unused")
            override suspend fun verifyAccount(email: String, otp: String): Resource<String> = error("unused")
            override suspend fun resendSignupOtp(email: String): Resource<String> = error("unused")
            override suspend fun forgotPassword(email: String): Resource<ForgotPasswordResult> = error("unused")
            override suspend fun resetPassword(email: String, otp: String, newPassword: String, confirmPassword: String, encryptedVaultKey: String?): Resource<String> = error("unused")
        }
        val subscriptions = PendingPaymentRepository(pending = true)

        SignOutUseCase(auth, subscriptions)()

        assertTrue(sessionCleared)
        assertFalse(subscriptions.pending)
    }

    private class PendingPaymentRepository(var pending: Boolean) : SubscriptionRepository {
        override suspend fun isPaymentPending() = pending
        override suspend fun setPaymentPending(pending: Boolean) {
            this.pending = pending
        }
        override suspend fun getAccountSummary() = Resource.Error(DataError.NoInternet)
        override suspend fun getBilling() = Resource.Error(DataError.NoInternet)
        override suspend fun isTrialEligible() = Resource.Error(DataError.NoInternet)
        override suspend fun startTrial(plan: SubscriptionPlan) = Resource.Error(DataError.NoInternet)
        override suspend fun createSubscription(plan: SubscriptionPlan) = Resource.Error(DataError.NoInternet)
        override suspend fun cancelSubscription(): Resource<CancelledSubscription> = Resource.Error(DataError.NoInternet)
    }
}
