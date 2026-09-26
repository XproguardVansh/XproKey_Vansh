package com.xprokeey2.data.repository

import com.xprokeey2.data.local.session.SessionStorage
import com.xprokeey2.data.mapper.toDomain
import com.xprokeey2.data.remote.datasource.LicenseRemoteDataSource
import com.xprokeey2.data.remote.dto.license.ActivateLicenseRequestDto
import com.xprokeey2.domain.model.LicenseActivation
import com.xprokeey2.domain.repository.LicenseRepository
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import java.net.HttpURLConnection
import javax.inject.Inject

class LicenseRepositoryImpl @Inject constructor(
    private val remote: LicenseRemoteDataSource,
    private val sessionStorage: SessionStorage,
) : LicenseRepository {

    override suspend fun activateLicense(keyCode: String): Resource<LicenseActivation> {
        // Both come from the login response saved on this device.
        val accessToken = sessionStorage.getAccessToken()
        val userId = sessionStorage.getUserId()
        if (accessToken.isNullOrBlank() || userId.isNullOrBlank()) {
            return Resource.Error(DataError.SessionExpired)
        }

        val request = ActivateLicenseRequestDto(userId = userId, keyCode = keyCode)
        return when (val result = remote.activateLicense(accessToken, request)) {
            is Resource.Success -> Resource.Success(result.data.toDomain())
            is Resource.Error -> {
                val error = result.error
                if (error is DataError.Server && error.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    Resource.Error(DataError.SessionExpired)
                } else {
                    result
                }
            }
        }
    }
}
