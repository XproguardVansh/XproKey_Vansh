package com.xprokeey2.domain.usecase.user

import com.xprokeey2.domain.model.User
import com.xprokeey2.domain.repository.UserRepository
import javax.inject.Inject

class GetSignedInUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(): User? = userRepository.getSignedInUser()
}
