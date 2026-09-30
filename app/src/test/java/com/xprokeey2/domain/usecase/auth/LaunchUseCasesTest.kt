package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.data.local.session.InMemoryVaultSession
import com.xprokeey2.domain.model.CancelledSubscription
import com.xprokeey2.domain.model.EncryptedVaultKeys
import com.xprokeey2.domain.model.ForgotPasswordResult
import com.xprokeey2.domain.model.LaunchDestination
import com.xprokeey2.domain.model.LoginResult
import com.xprokeey2.domain.model.ServerSessionTimeout
import com.xprokeey2.domain.model.SessionTimeoutSettings
import com.xprokeey2.domain.model.SignupResult
import com.xprokeey2.domain.model.SubscriptionPlan
import com.xprokeey2.domain.model.TimeoutAction
import com.xprokeey2.domain.model.TimeoutDuration
import com.xprokeey2.domain.repository.AuthRepository
import com.xprokeey2.domain.repository.SecuritySettingsRepository
import com.xprokeey2.domain.repository.SessionRepository
import com.xprokeey2.domain.repository.SubscriptionRepository
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reopening the app: straight in like the web, unless the Settings > Security timeout says otherwise. */
class LaunchUseCasesTest {

    private class FakeSession(
        private val signedIn: Boolean,
        private val encryptedVaultKey: String? = "encrypted-key",
        private val masterSalt: String? = "salt",
    ) : SessionRepository {
        override suspend fun hasSession() = signedIn
        override suspend fun getMasterSalt() = masterSalt
        override suspend fun getEncryptedVaultKey() = encryptedVaultKey
        override suspend fun saveEncryptedVaultKey(encryptedVaultKey: String) = error("unused")
    }

    private class FakeSecurity(
        private val saved: SessionTimeoutSettings?,
        private val lastActivity: Long?,
    ) : SecuritySettingsRepository {
        override suspend fun fetchSettings(): Resource<ServerSessionTimeout> = error("unused")
        override suspend fun updateSettings(settings: SessionTimeoutSettings): Resource<Unit> = error("unused")
        override suspend fun getSavedSettings() = saved
        override suspend fun saveSettings(settings: SessionTimeoutSettings) = error("unused")
        override fun observeSavedSettings(): Flow<SessionTimeoutSettings?> = flowOf(saved)
        override suspend fun getLastActivity() = lastActivity
        override suspend fun saveLastActivity(epochMillis: Long) = error("unused")
    }

    private class FakeAuth : AuthRepository {
        var sessionCleared = false

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

    private class NoPayments : SubscriptionRepository {
        override suspend fun isPaymentPending() = false
        override suspend fun setPaymentPending(pending: Boolean) = Unit
        override suspend fun getAccountSummary() = Resource.Error(DataError.NoInternet)
        override suspend fun getBilling() = Resource.Error(DataError.NoInternet)
        override suspend fun isTrialEligible() = Resource.Error(DataError.NoInternet)
        override suspend fun startTrial(plan: SubscriptionPlan) = Resource.Error(DataError.NoInternet)
        override suspend fun createSubscription(plan: SubscriptionPlan) = Resource.Error(DataError.NoInternet)
        override suspend fun cancelSubscription(): Resource<CancelledSubscription> = Resource.Error(DataError.NoInternet)
    }

    private val now = 1_000_000_000L
    private val minute = 60_000L

    private fun launch(
        signedIn: Boolean = true,
        settings: SessionTimeoutSettings? = null,
        lastActivity: Long? = now - minute,
        auth: FakeAuth = FakeAuth(),
    ) = runBlocking {
        GetLaunchDestinationUseCase(
            FakeSession(signedIn),
            FakeSecurity(settings, lastActivity),
            SignOutUseCase(auth, NoPayments()),
        )(nowMillis = now)
    }

    @Test
    fun nobodySignedInOpensLogin() {
        assertEquals(LaunchDestination.LOGIN, launch(signedIn = false))
    }

    @Test
    fun signedInGoesStraightInWithNeverOrTheWebsDefault() {
        assertEquals(LaunchDestination.APP, launch(settings = null, lastActivity = null))
        assertEquals(LaunchDestination.APP, launch(settings = SessionTimeoutSettings(TimeoutDuration.NEVER, TimeoutAction.LOCK), lastActivity = now - 90 * 24 * 60 * minute))
    }

    @Test
    fun withinTheTimeoutStillGoesStraightIn() {
        val settings = SessionTimeoutSettings(TimeoutDuration.FIFTEEN_MINUTES, TimeoutAction.LOGOUT)
        assertEquals(LaunchDestination.APP, launch(settings = settings, lastActivity = now - 14 * minute))
    }

    @Test
    fun afterTheTimeoutLockShowsTheLockScreen() {
        val auth = FakeAuth()
        val settings = SessionTimeoutSettings(TimeoutDuration.FIVE_MINUTES, TimeoutAction.LOCK)

        assertEquals(LaunchDestination.LOCK, launch(settings = settings, lastActivity = now - 5 * minute, auth = auth))
        assertFalse(auth.sessionCleared)
    }

    @Test
    fun afterTheTimeoutLogOutSignsOutAndShowsLogin() {
        val auth = FakeAuth()
        val settings = SessionTimeoutSettings(TimeoutDuration.ONE_HOUR, TimeoutAction.LOGOUT)

        assertEquals(LaunchDestination.LOGIN, launch(settings = settings, lastActivity = now - 2 * 60 * minute, auth = auth))
        assertTrue(auth.sessionCleared)
    }

    @Test
    fun unknownOrImpossibleIdleTimeCountsAsTimedOut() {
        val settings = SessionTimeoutSettings(TimeoutDuration.FOUR_HOURS, TimeoutAction.LOCK)
        assertEquals(LaunchDestination.LOCK, launch(settings = settings, lastActivity = null))
        assertEquals(LaunchDestination.LOCK, launch(settings = settings, lastActivity = now + minute))
    }

    @Test
    fun signInAgainOnlyNeedsTheMasterPasswordWhenJustTheVaultKeyIsMissing() = runBlocking {
        val locked = InMemoryVaultSession()
        val unlocked = InMemoryVaultSession().apply { unlock("vault-key") }

        assertTrue(NeedsUnlockUseCase(FakeSession(signedIn = true), locked)())
        assertFalse(NeedsUnlockUseCase(FakeSession(signedIn = true), unlocked)())
        assertFalse(NeedsUnlockUseCase(FakeSession(signedIn = false), locked)())
        assertFalse(NeedsUnlockUseCase(FakeSession(signedIn = true, encryptedVaultKey = ""), locked)())
        assertFalse(NeedsUnlockUseCase(FakeSession(signedIn = true, masterSalt = null), locked)())
    }
}
