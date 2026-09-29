package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.local.session.AccessTokenStore
import com.xprokeey2.data.remote.api.TransferApi
import com.xprokeey2.data.remote.dto.transfer.ImportItemDto
import com.xprokeey2.data.remote.dto.transfer.ImportResponseDto
import com.xprokeey2.data.remote.util.authorizedApiCall
import com.xprokeey2.domain.model.ExportFormat
import com.xprokeey2.domain.util.Resource
import kotlinx.serialization.json.Json
import javax.inject.Inject

class TransferRemoteDataSourceImpl @Inject constructor(
    private val api: TransferApi,
    private val tokenStore: AccessTokenStore,
    private val json: Json,
) : TransferRemoteDataSource {

    override suspend fun download(format: ExportFormat): Resource<ByteArray> =
        authorizedApiCall(tokenStore, json) { authorization ->
            val body = when (format) {
                ExportFormat.CSV -> api.exportCsv(authorization)
                ExportFormat.JSON -> api.exportJson(authorization)
                ExportFormat.ENCRYPTED -> api.exportEncrypted(authorization)
            }
            body.use { it.bytes() }
        }

    override suspend fun import(items: List<ImportItemDto>): Resource<ImportResponseDto> =
        authorizedApiCall(tokenStore, json) { api.importItems(it, items) }
}
