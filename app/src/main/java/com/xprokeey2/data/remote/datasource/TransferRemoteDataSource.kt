package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.remote.dto.transfer.ImportItemDto
import com.xprokeey2.data.remote.dto.transfer.ImportResponseDto
import com.xprokeey2.domain.model.ExportFormat
import com.xprokeey2.domain.util.Resource

interface TransferRemoteDataSource {
    /** The export file's bytes, as sent. */
    suspend fun download(format: ExportFormat): Resource<ByteArray>

    suspend fun import(items: List<ImportItemDto>): Resource<ImportResponseDto>
}
