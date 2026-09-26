package com.xprokeey2.data.remote.api

import com.xprokeey2.data.remote.dto.license.ActivateLicenseRequestDto
import com.xprokeey2.data.remote.dto.license.ActivateLicenseResponseDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface LicenseApi {

    @POST("license/activate")
    suspend fun activateLicense(
        @Header("Authorization") authorization: String,
        @Body body: ActivateLicenseRequestDto,
    ): ActivateLicenseResponseDto
}
