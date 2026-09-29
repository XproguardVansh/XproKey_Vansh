package com.xprokeey2.domain.usecase.transfer

import com.xprokeey2.domain.model.PickedFile
import com.xprokeey2.domain.repository.UserFileRepository
import com.xprokeey2.domain.repository.VaultTransferRepository
import com.xprokeey2.domain.security.VaultCrypto
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * Tools > Import: reads a .csv, .json or .xpk file like the web, encrypts the plain passwords of
 * CSV and JSON with the vault key (an .xpk's are encrypted already) and sends all of them to
 * POST /import. Returns how many items were imported.
 */
class ImportVaultUseCase @Inject constructor(
    private val transferRepository: VaultTransferRepository,
    private val userFiles: UserFileRepository,
    private val vaultCrypto: VaultCrypto,
    private val vaultSession: VaultSession,
) {
    suspend operator fun invoke(file: PickedFile): Resource<Int> {
        val vaultKey = vaultSession.vaultKey ?: return Resource.Error(DataError.VaultLocked)
        val text = when (val result = userFiles.read(file.uri)) {
            // Like the browser's file.text(): UTF-8, without a byte order mark.
            is Resource.Success -> result.data.decodeToString().removePrefix("﻿")
            is Resource.Error -> return result
        }
        val parsed = try {
            VaultFileFormat.parseImport(file.name, text)
        } catch (e: ImportFileException) {
            return Resource.Error(DataError.ImportFile(e.problem))
        }
        val records = if (parsed.passwordsEncrypted) {
            parsed.records
        } else {
            parsed.records.map { it.copy(password = vaultCrypto.encryptWithVaultKey(it.password, vaultKey)) }
        }
        return transferRepository.import(records)
    }
}
