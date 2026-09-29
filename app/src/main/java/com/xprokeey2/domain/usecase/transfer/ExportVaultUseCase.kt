package com.xprokeey2.domain.usecase.transfer

import com.xprokeey2.domain.model.ExportFormat
import com.xprokeey2.domain.repository.UserFileRepository
import com.xprokeey2.domain.repository.VaultTransferRepository
import com.xprokeey2.domain.security.VaultCrypto
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * Tools > Export: downloads [ExportFormat] from the server and saves it into the file the user just
 * created with "Save as". Like the web, CSV and JSON get their passwords decrypted first and the
 * .xpk is saved as it is. If anything fails, that file is removed again so no empty export is left.
 */
class ExportVaultUseCase @Inject constructor(
    private val transferRepository: VaultTransferRepository,
    private val userFiles: UserFileRepository,
    private val vaultCrypto: VaultCrypto,
    private val vaultSession: VaultSession,
) {
    suspend operator fun invoke(format: ExportFormat, destinationUri: String): Resource<Unit> {
        val result = export(format, destinationUri)
        if (result is Resource.Error) userFiles.delete(destinationUri)
        return result
    }

    private suspend fun export(format: ExportFormat, destinationUri: String): Resource<Unit> {
        val vaultKey = vaultSession.vaultKey ?: return Resource.Error(DataError.VaultLocked)
        val download = when (val result = transferRepository.download(format)) {
            is Resource.Success -> result.data
            is Resource.Error -> return result
        }
        // The web's decryptWithVaultKey gives "" for a password it can't decrypt.
        val decrypt: suspend (String) -> String = { vaultCrypto.decryptWithVaultKey(it, vaultKey).orEmpty() }
        val content = try {
            when (format) {
                ExportFormat.CSV -> VaultFileFormat.decryptCsvExport(download.decodeToString(), decrypt).encodeToByteArray()
                ExportFormat.JSON -> VaultFileFormat.decryptJsonExport(download.decodeToString(), decrypt).encodeToByteArray()
                ExportFormat.ENCRYPTED -> download
            }
        } catch (e: IllegalArgumentException) {
            // Not the JSON the server normally sends (also covers SerializationException).
            return Resource.Error(DataError.Unknown(e.message))
        } catch (e: NoSuchElementException) {
            return Resource.Error(DataError.Unknown(e.message))
        }
        return userFiles.write(destinationUri, content)
    }
}
