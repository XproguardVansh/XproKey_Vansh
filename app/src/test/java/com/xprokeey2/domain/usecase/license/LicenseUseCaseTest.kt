package com.xprokeey2.domain.usecase.license

import com.xprokeey2.domain.model.LicenseActivation
import com.xprokeey2.domain.repository.LicenseRepository
import com.xprokeey2.domain.usecase.validation.ValidateLicenseKeyUseCase
import com.xprokeey2.domain.usecase.validation.ValidationError
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LicenseUseCaseTest {

    private class FakeLicenseRepository : LicenseRepository {
        var sentKeyCode: String? = null

        override suspend fun activateLicense(keyCode: String): Resource<LicenseActivation> {
            sentKeyCode = keyCode
            return Resource.Success(LicenseActivation(message = "License activated successfully", organization = "Acme"))
        }
    }

    @Test
    fun activationTrimsWhitespaceButKeepsCaseAndSymbols() = runBlocking {
        val repository = FakeLicenseRepository()

        ActivateLicenseUseCase(repository)("  3TFbIEW%LGr-\n")

        assertEquals("3TFbIEW%LGr-", repository.sentKeyCode)
    }

    @Test
    fun blankKeyIsRequired() {
        val validate = ValidateLicenseKeyUseCase()

        assertEquals(ValidationError.REQUIRED, validate(""))
        assertEquals(ValidationError.REQUIRED, validate("   "))
        assertNull(validate("3TFbIEW%LGr-"))
    }
}
