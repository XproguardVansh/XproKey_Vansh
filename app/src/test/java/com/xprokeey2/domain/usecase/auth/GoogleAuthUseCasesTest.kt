package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.data.crypto.VaultCryptoImpl
import com.xprokeey2.domain.model.EncryptedVaultKeys
import com.xprokeey2.domain.model.GoogleAuthOutcome
import com.xprokeey2.domain.model.GoogleSession
import com.xprokeey2.domain.model.GoogleSignInStep
import com.xprokeey2.domain.model.MasterPasswordStrength
import com.xprokeey2.domain.model.User
import com.xprokeey2.domain.repository.GoogleAuthRepository
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.usecase.validation.ValidateVaultSetupFormUseCase
import com.xprokeey2.domain.usecase.validation.VaultSetupFormError
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthUseCasesTest {

    private class FakeGoogleAuthRepository(
        private val signInResult: Resource<GoogleAuthOutcome> = Resource.Error(DataError.Unknown()),
        private val setupResult: Resource<GoogleSession?> = Resource.Error(DataError.Unknown()),
    ) : GoogleAuthRepository {
        var saved: GoogleSession? = null
        var sentKeys: EncryptedVaultKeys? = null
        var sentSetupToken: String? = null

        override suspend fun signIn(idToken: String) = signInResult

        override suspend fun setupVaultPassword(
            setupToken: String,
            password: String,
            confirmPassword: String,
            vaultKeys: EncryptedVaultKeys,
        ): Resource<GoogleSession?> {
            sentSetupToken = setupToken
            sentKeys = vaultKeys
            return setupResult
        }

        override suspend fun saveSession(session: GoogleSession) {
            saved = session
        }
    }

    private class FakeVaultSession : VaultSession {
        override var vaultKey: String? = "stale"
        override fun unlock(vaultKey: String) {
            this.vaultKey = vaultKey
        }

        override fun lock() {
            vaultKey = null
        }
    }

    private val crypto = VaultCryptoImpl()

    private fun session(
        masterSalt: String = "",
        encryptedVaultKey: String = "",
        nextAction: String? = "dashboard",
        user: User = User(userId = "0507", name = "Vansh", email = "goelv2610@gmail.com"),
    ) = GoogleSession(
        accessToken = "acc",
        refreshToken = "ref",
        user = user,
        nextAction = nextAction,
        masterSalt = masterSalt,
        encryptedVaultKey = encryptedVaultKey,
    )

    @Test
    fun newAccountIsAskedToCreateItsMasterPassword() = runBlocking {
        val repository = FakeGoogleAuthRepository(
            signInResult = Resource.Success(GoogleAuthOutcome.VaultSetupRequired("setup-1", "Vansh", "v@x.com")),
        )

        val result = SignInWithGoogleUseCase(repository, FakeVaultSession())("id-token")

        assertEquals(Resource.Success(GoogleSignInStep.CreateMasterPassword("setup-1", "Vansh", "v@x.com")), result)
        assertNull(repository.saved)
    }

    @Test
    fun accountWithVaultIsNotSavedBeforeItsMasterPassword() = runBlocking {
        val withVault = session(masterSalt = "salt", encryptedVaultKey = "key")
        val repository = FakeGoogleAuthRepository(signInResult = Resource.Success(GoogleAuthOutcome.SignedIn(withVault, "ok")))

        val result = SignInWithGoogleUseCase(repository, FakeVaultSession())("id-token")

        assertEquals(Resource.Success(GoogleSignInStep.EnterMasterPassword(withVault)), result)
        assertNull(repository.saved)
    }

    @Test
    fun accountWithoutVaultIsSignedInWithTheVaultLocked() = runBlocking {
        val withoutVault = session(nextAction = "payment")
        val repository = FakeGoogleAuthRepository(signInResult = Resource.Success(GoogleAuthOutcome.SignedIn(withoutVault, "ok")))
        val vaultSession = FakeVaultSession()

        val result = SignInWithGoogleUseCase(repository, vaultSession)("id-token")

        assertEquals(Resource.Success(GoogleSignInStep.SignedIn(needsAccountSetup = true, message = "ok")), result)
        assertEquals(withoutVault, repository.saved)
        assertNull(vaultSession.vaultKey)
    }

    @Test
    fun serverErrorsPassThrough() = runBlocking {
        val error = Resource.Error(DataError.Server(401, "Invalid Google token"))
        val result = SignInWithGoogleUseCase(FakeGoogleAuthRepository(signInResult = error), FakeVaultSession())("bad")
        assertEquals(error, result)
    }

    @Test
    fun masterPasswordOpensTheVaultThenSavesTheSession() = runBlocking {
        val vault = crypto.createVault("Vansh@2610")
        val pending = session(
            masterSalt = vault.encryptedKeys.masterSalt,
            encryptedVaultKey = vault.encryptedKeys.encryptedVaultKey,
        )
        val repository = FakeGoogleAuthRepository()
        val vaultSession = FakeVaultSession()
        val unlock = UnlockGoogleVaultUseCase(repository, crypto, vaultSession)

        assertFalse(unlock(pending, "wrong password"))
        assertNull(repository.saved)
        assertEquals("stale", vaultSession.vaultKey)

        assertTrue(unlock(pending, "Vansh@2610"))
        assertEquals(pending, repository.saved)
        assertEquals(vault.vaultKey, vaultSession.vaultKey)
    }

    @Test
    fun setupSavesTheSessionWithTheNewVaultOpen() = runBlocking {
        // The setup answer without vault keys or user details: the ones just made are used.
        val returned = session(nextAction = "payment", user = User(userId = "0507", name = "", email = ""))
        val repository = FakeGoogleAuthRepository(setupResult = Resource.Success(returned))
        val vaultSession = FakeVaultSession()

        val result = SetupGoogleVaultUseCase(repository, crypto, vaultSession)(
            setupToken = "setup-1",
            password = "Vansh@2610",
            confirmPassword = "Vansh@2610",
            name = "Vansh Goel",
            email = "goelv2610@gmail.com",
        )

        val setup = (result as Resource.Success).data
        assertTrue(setup.isSignedIn)
        assertTrue(setup.needsAccountSetup)
        assertEquals("setup-1", repository.sentSetupToken)

        val sent = repository.sentKeys!!
        val saved = repository.saved!!
        assertEquals(sent.masterSalt, saved.masterSalt)
        assertEquals(sent.encryptedVaultKey, saved.encryptedVaultKey)
        assertEquals("Vansh Goel", saved.user.name)
        assertEquals("goelv2610@gmail.com", saved.user.email)

        // The vault key in memory is the one the master password and the recovery key both open.
        val vaultKey = vaultSession.vaultKey
        assertEquals(vaultKey, crypto.unlockVaultKey("Vansh@2610", sent.masterSalt, sent.encryptedVaultKey))
        assertEquals(vaultKey, crypto.recoverVaultKey(setup.recoveryKey, sent.masterSalt, sent.encryptedVaultKeyRecovery))
        assertFalse(setup.toString().contains(setup.recoveryKey))
    }

    @Test
    fun setupWithoutSessionInTheAnswerStillGivesTheRecoveryKey() = runBlocking {
        val repository = FakeGoogleAuthRepository(setupResult = Resource.Success(null))
        val vaultSession = FakeVaultSession()

        val result = SetupGoogleVaultUseCase(repository, crypto, vaultSession)("setup-1", "Vansh@2610", "Vansh@2610", "", "")

        val setup = (result as Resource.Success).data
        assertFalse(setup.isSignedIn)
        assertTrue(setup.recoveryKey.isNotBlank())
        assertNull(repository.saved)
        assertEquals("stale", vaultSession.vaultKey)
    }

    @Test
    fun failedSetupSavesNothing() = runBlocking {
        val error = Resource.Error(DataError.Server(401, "Invalid or expired setup token"))
        val repository = FakeGoogleAuthRepository(setupResult = error)

        val result = SetupGoogleVaultUseCase(repository, crypto, FakeVaultSession())("setup-1", "Vansh@2610", "Vansh@2610", "", "")

        assertEquals(error, result)
        assertNull(repository.saved)
    }

    @Test
    fun setupFormChecksLengthThenMatch() {
        val validate = ValidateVaultSetupFormUseCase()
        assertEquals(VaultSetupFormError.PASSWORD_TOO_SHORT, validate("short", "other"))
        assertEquals(VaultSetupFormError.PASSWORD_MISMATCH, validate("long enough", "different"))
        assertNull(validate("long enough", "long enough"))
    }

    @Test
    fun strengthFollowsTheWebScore() {
        assertEquals(MasterPasswordStrength.WEAK, MasterPasswordStrength.of(""))
        assertEquals(MasterPasswordStrength.WEAK, MasterPasswordStrength.of("abcdefgh"))
        // A space counts as a symbol, like the web's /[^A-Za-z0-9]/.
        assertEquals(MasterPasswordStrength.WEAK, MasterPasswordStrength.of("ab 1"))
        assertEquals(MasterPasswordStrength.MEDIUM, MasterPasswordStrength.of("Abcdefgh1"))
        assertEquals(MasterPasswordStrength.MEDIUM, MasterPasswordStrength.of("Abcdefgh1!"))
        assertEquals(MasterPasswordStrength.STRONG, MasterPasswordStrength.of("Abcdefghij1!"))
    }
}
