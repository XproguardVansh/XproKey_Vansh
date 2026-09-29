package com.xprokeey2.domain.usecase.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VaultItemFormValidationTest {

    private val validate = ValidateVaultItemFormUseCase()

    @Test
    fun validItemPasses() {
        assertFalse(validate("Github", "https://github.com", "Vanshgoel2610", "cVkd,FO)w[n09vR8").hasErrors)
    }

    @Test
    fun nameUrlUsernameAndPasswordAreRequired() {
        val errors = validate(" ", "", "", "")
        assertEquals(ValidationError.REQUIRED, errors.title)
        assertEquals(ValidationError.REQUIRED, errors.url)
        assertEquals(ValidationError.REQUIRED, errors.username)
        assertEquals(ValidationError.REQUIRED, errors.password)
    }

    @Test
    fun anyWebsiteAddressIsAccepted() {
        // The web only marks the field as required.
        assertEquals(null, validate("Github", "github.com", "u", "p").url)
        assertEquals(null, validate("Github", "http://github.com/login", "u", "p").url)
    }
}
