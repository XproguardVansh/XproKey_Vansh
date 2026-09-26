package com.xprokeey2.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginResultTest {

    private fun loginResult(nextAction: String?) = LoginResult(
        message = "Login successful",
        user = User(userId = "0519", name = "Test", email = "t@example.com"),
        nextAction = nextAction,
        masterSalt = "",
        encryptedVaultKey = "",
    )

    @Test
    fun dashboardSkipsAccountSetup() {
        assertFalse(loginResult(nextAction = "dashboard").needsAccountSetup)
    }

    @Test
    fun anythingElseShowsAccountSetup() {
        assertTrue(loginResult(nextAction = "payment").needsAccountSetup)
        assertTrue(loginResult(nextAction = null).needsAccountSetup)
    }
}
