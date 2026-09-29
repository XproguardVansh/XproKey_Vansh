package com.xprokeey2.domain.repository

import com.xprokeey2.domain.model.ExportFormat
import com.xprokeey2.domain.model.ImportRecord
import com.xprokeey2.domain.util.Resource

interface VaultTransferRepository {

    /** GET /export/csv, /export/json or /export/encrypted: the file exactly as the server sends it. */
    suspend fun download(format: ExportFormat): Resource<ByteArray>

    /** POST /import; returns how many items the server imported. */
    suspend fun import(records: List<ImportRecord>): Resource<Int>
}
