package com.xprokeey2.domain.usecase.account

import com.xprokeey2.data.mapper.toDomain
import com.xprokeey2.data.remote.dto.account.MeDto
import com.xprokeey2.domain.model.AccountProfile
import com.xprokeey2.domain.model.Billing
import com.xprokeey2.domain.model.CancelledSubscription
import com.xprokeey2.domain.model.EncryptedVaultKeys
import com.xprokeey2.domain.model.ForgotPasswordResult
import com.xprokeey2.domain.model.LoginResult
import com.xprokeey2.domain.model.SignupResult
import com.xprokeey2.domain.model.SubscriptionPlan
import com.xprokeey2.domain.repository.AccountRepository
import com.xprokeey2.domain.repository.AuthRepository
import com.xprokeey2.domain.repository.SubscriptionRepository
import com.xprokeey2.domain.usecase.auth.LogOutUseCase
import com.xprokeey2.domain.usecase.auth.SignOutUseCase
import com.xprokeey2.domain.usecase.subscription.GetBillingUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** The Profile page's data and the account menu's Log out. */
class ProfileUseCasesTest {

    private class FakeAccounts(
        private val profile: Resource<AccountProfile>,
        private val logOutDelayMillis: Long = 0,
        private val logOutResult: Resource<Unit> = Resource.Success(Unit),
    ) : AccountRepository {
        var logOutCalls = 0

        override suspend fun getProfile() = profile
        override suspend fun logOut(): Resource<Unit> {
            logOutCalls++
            delay(logOutDelayMillis)
            return logOutResult
        }
    }

    private class FakeSubscriptions(private val billing: Resource<Billing>) : SubscriptionRepository {
        var billingCalls = 0
        var pending = true

        override suspend fun getBilling(): Resource<Billing> {
            billingCalls++
            return billing
        }
        override suspend fun isPaymentPending() = pending
        override suspend fun setPaymentPending(pending: Boolean) {
            this.pending = pending
        }
        override suspend fun getAccountSummary() = Resource.Error(DataError.NoInternet)
        override suspend fun isTrialEligible() = Resource.Error(DataError.NoInternet)
        override suspend fun startTrial(plan: SubscriptionPlan) = Resource.Error(DataError.NoInternet)
        override suspend fun createSubscription(plan: SubscriptionPlan) = Resource.Error(DataError.NoInternet)
        override suspend fun cancelSubscription(): Resource<CancelledSubscription> = Resource.Error(DataError.NoInternet)
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

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private fun profile(accountType: String) = AccountProfile(
        email = "goelv2610@gmail.com", name = "", accountType = accountType, joinedAt = null,
        organizationName = "kk", licenseStatus = "active", licenseExpiresAt = null,
    )

    private val activeBilling = Billing(
        planType = "yearly", subscriptionStatus = "active", isTrial = false, expiresAt = null,
        nextBillingAt = null, trialDaysLeft = null, isExpiringSoon = false, autoRenew = false,
    )

    @Test
    fun meResponseLikeTheWebsUserType() {
        val me = json.decodeFromString<MeDto>(
            """{"id":507,"email":"goelv2610@gmail.com","account_type":"business","name":null,"created_at":"2026-09-25T10:15:00Z",
               "organization_name":"kk","license_status":"active","license_expires_at":"2027-09-26T00:00:00+05:30"}"""
        ).toDomain()

        assertEquals("goelv2610@gmail.com", me.email)
        assertEquals("", me.name)
        assertTrue(me.isBusiness)
        assertEquals(Instant.parse("2026-09-25T10:15:00Z"), me.joinedAt)
        assertEquals("kk", me.organizationName)
        assertEquals(Instant.parse("2027-09-25T18:30:00Z"), me.licenseExpiresAt)
        assertEquals(
            Instant.parse("2026-09-30T00:00:00Z"),
            json.decodeFromString<MeDto>("""{"email":"a@b.c","createdAt":"2026-09-30T00:00:00Z"}""").toDomain().joinedAt,
        )
    }

    @Test
    fun personalProfileAlsoReadsBilling() = runBlocking {
        val subscriptions = FakeSubscriptions(Resource.Success(activeBilling))
        val result = GetProfileUseCase(FakeAccounts(Resource.Success(profile("personal"))), GetBillingUseCase(subscriptions))()

        assertEquals(activeBilling, (result as Resource.Success).data.billing)
        assertEquals(1, subscriptions.billingCalls)
    }

    @Test
    fun businessProfileSkipsBillingAndFailuresOnlyDropIt() = runBlocking {
        val forBusiness = FakeSubscriptions(Resource.Success(activeBilling))
        val business = GetProfileUseCase(FakeAccounts(Resource.Success(profile("business"))), GetBillingUseCase(forBusiness))()
        assertNull((business as Resource.Success).data.billing)
        assertEquals(0, forBusiness.billingCalls)

        val failing = FakeSubscriptions(Resource.Error(DataError.Server(500, "boom")))
        val personal = GetProfileUseCase(FakeAccounts(Resource.Success(profile("personal"))), GetBillingUseCase(failing))()
        assertNull((personal as Resource.Success).data.billing)

        val noAccount = GetProfileUseCase(FakeAccounts(Resource.Error(DataError.SessionExpired)), GetBillingUseCase(failing))()
        assertEquals(DataError.SessionExpired, (noAccount as Resource.Error).error)
    }

    @Test
    fun logOutTellsTheServerThenForgetsTheSessionWhateverItSaid() = runBlocking {
        val accounts = FakeAccounts(Resource.Success(profile("personal")), logOutResult = Resource.Error(DataError.NoInternet))
        val auth = FakeAuth()
        val subscriptions = FakeSubscriptions(Resource.Success(activeBilling))

        LogOutUseCase(accounts, SignOutUseCase(auth, subscriptions))()

        assertEquals(1, accounts.logOutCalls)
        assertTrue(auth.sessionCleared)
        assertEquals(false, subscriptions.pending)
    }

    @Test
    fun aSlowServerDoesNotHoldTheLogOutBack() = runBlocking {
        val accounts = FakeAccounts(Resource.Success(profile("personal")), logOutDelayMillis = 60_000)
        val auth = FakeAuth()
        val started = System.nanoTime()

        LogOutUseCase(accounts, SignOutUseCase(auth, FakeSubscriptions(Resource.Success(activeBilling))))(serverTimeoutMillis = 50)

        assertTrue(auth.sessionCleared)
        assertTrue((System.nanoTime() - started) / 1_000_000 < 5_000)
    }
}
