package com.xprokeey2.domain.usecase.license

import com.xprokeey2.domain.model.LicenseActivation
import com.xprokeey2.domain.repository.LicenseRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

class ActivateLicenseUseCase @Inject constructor(
    private val licenseRepository: LicenseRepository,
) {
    /** Keys are case-sensitive and may contain symbols (e.g. "3TFbIEW%LGr-"), so only whitespace is trimmed. */
    suspend operator fun invoke(keyCode: String): Resource<LicenseActivation> =
        licenseRepository.activateLicense(keyCode = keyCode.trim())
}
