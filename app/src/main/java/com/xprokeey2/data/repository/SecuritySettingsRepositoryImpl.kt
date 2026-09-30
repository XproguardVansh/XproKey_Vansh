package com.xprokeey2.data.repository

import com.xprokeey2.data.local.security.SessionTimeoutStorage
import com.xprokeey2.data.mapper.toDomain
import com.xprokeey2.data.mapper.toDto
import com.xprokeey2.data.remote.datasource.AccountRemoteDataSource
import com.xprokeey2.domain.model.ServerSessionTimeout
import com.xprokeey2.domain.model.SessionTimeoutSettings
import com.xprokeey2.domain.repository.SecuritySettingsRepository
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.domain.util.map
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SecuritySettingsRepositoryImpl @Inject constructor(
    private val remote: AccountRemoteDataSource,
    private val storage: SessionTimeoutStorage,
) : SecuritySettingsRepository {

    override suspend fun fetchSettings(): Resource<ServerSessionTimeout> = remote.getSecuritySettings().map { it.toDomain() }

    override suspend fun updateSettings(settings: SessionTimeoutSettings): Resource<Unit> =
        remote.updateSecuritySettings(settings.toDto())

    override suspend fun getSavedSettings(): SessionTimeoutSettings? = storage.get()

    override suspend fun saveSettings(settings: SessionTimeoutSettings) = storage.save(settings)

    override fun observeSavedSettings(): Flow<SessionTimeoutSettings?> = storage.settings

    override suspend fun getLastActivity(): Long? = storage.getLastActivity()

    override suspend fun saveLastActivity(epochMillis: Long) = storage.saveLastActivity(epochMillis)
}
