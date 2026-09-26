package com.xprokeey2.domain.repository

import com.xprokeey2.domain.model.LicenseActivation
import com.xprokeey2.domain.util.Resource

interface LicenseRepository {

    /** Activates a business license key for the signed-in user. */
    suspend fun activateLicense(keyCode: String): Resource<LicenseActivation>
}
