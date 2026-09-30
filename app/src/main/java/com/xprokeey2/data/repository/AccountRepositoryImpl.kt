package com.xprokeey2.data.repository

import com.xprokeey2.data.mapper.toDomain
import com.xprokeey2.data.remote.datasource.AccountRemoteDataSource
import com.xprokeey2.domain.model.AccountProfile
import com.xprokeey2.domain.repository.AccountRepository
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.domain.util.map
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val remote: AccountRemoteDataSource,
) : AccountRepository {

    override suspend fun getProfile(): Resource<AccountProfile> = remote.getMe().map { it.toDomain() }

    override suspend fun logOut(): Resource<Unit> = remote.logout()
}
