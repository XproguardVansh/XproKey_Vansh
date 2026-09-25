package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.domain.repository.AuthRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

class ResendSignupOtpUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(email: String): Resource<String> =
        repository.resendSignupOtp(email = email.trim())
}
