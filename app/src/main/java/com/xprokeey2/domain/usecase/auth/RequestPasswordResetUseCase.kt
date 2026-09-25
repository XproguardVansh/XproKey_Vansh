package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.domain.model.ForgotPasswordResult
import com.xprokeey2.domain.repository.AuthRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/** Step 1 of "forgot password": the server emails an OTP. */
class RequestPasswordResetUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String): Resource<ForgotPasswordResult> =
        authRepository.forgotPassword(email = email.trim())
}
