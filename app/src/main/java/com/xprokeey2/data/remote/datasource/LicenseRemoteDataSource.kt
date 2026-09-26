package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.remote.dto.license.ActivateLicenseRequestDto
import com.xprokeey2.data.remote.dto.license.ActivateLicenseResponseDto
import com.xprokeey2.domain.util.Resource

interface LicenseRemoteDataSource {
    suspend fun activateLicense(
        accessToken: String,
        request: ActivateLicenseRequestDto,
    ): Resource<ActivateLicenseResponseDto>
}
