package com.xprokeey2.data.repository

import com.xprokeey2.data.mapper.toDto
import com.xprokeey2.data.remote.datasource.TransferRemoteDataSource
import com.xprokeey2.domain.model.ExportFormat
import com.xprokeey2.domain.model.ImportRecord
import com.xprokeey2.domain.repository.VaultTransferRepository
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.domain.util.map
import javax.inject.Inject

class VaultTransferRepositoryImpl @Inject constructor(
    private val remote: TransferRemoteDataSource,
) : VaultTransferRepository {

    override suspend fun download(format: ExportFormat): Resource<ByteArray> = remote.download(format)

    override suspend fun import(records: List<ImportRecord>): Resource<Int> =
        remote.import(records.map { it.toDto() }).map { it.imported ?: records.size }
}
