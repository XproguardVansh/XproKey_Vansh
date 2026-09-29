package com.xprokeey2.data.remote.api

import com.xprokeey2.data.remote.dto.transfer.ImportItemDto
import com.xprokeey2.data.remote.dto.transfer.ImportResponseDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/** Tools > Export and Import. */
interface TransferApi {

    @GET("export/csv")
    suspend fun exportCsv(@Header("Authorization") authorization: String): ResponseBody

    @GET("export/json")
    suspend fun exportJson(@Header("Authorization") authorization: String): ResponseBody

    /** The .xpk backup: JSON with the CSV (passwords still encrypted) in Base64. */
    @GET("export/encrypted")
    suspend fun exportEncrypted(@Header("Authorization") authorization: String): ResponseBody

    @POST("import")
    suspend fun importItems(
        @Header("Authorization") authorization: String,
        @Body items: List<ImportItemDto>,
    ): ImportResponseDto
}
