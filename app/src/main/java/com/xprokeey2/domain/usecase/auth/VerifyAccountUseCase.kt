package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.domain.model.VerifyAccountResult
import com.xprokeey2.domain.repository.AuthRepository
import com.xprokeey2.domain.repository.RecoveryKeyRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

class VerifyAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val recoveryKeyRepository: RecoveryKeyRepository,
) {
    suspend operator fun invoke(email: String, otp: String): Resource<VerifyAccountResult> =
        when (val result = authRepository.verifyAccount(email = email.trim(), otp = otp)) {
            is Resource.Success -> Resource.Success(
                VerifyAccountResult(
                    message = result.data,
                    recoveryKey = recoveryKeyRepository.getPendingKey(email.trim()),
                )
            )
            is Resource.Error -> result
        }
}
