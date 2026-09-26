package com.xprokeey2.data.repository

import com.xprokeey2.data.local.session.SessionStorage
import com.xprokeey2.domain.model.User
import com.xprokeey2.domain.repository.UserRepository
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val sessionStorage: SessionStorage,
) : UserRepository {

    override suspend fun getSignedInUser(): User? =
        sessionStorage.getProfile()?.let { User(userId = it.userId, name = it.name, email = it.email) }
}
