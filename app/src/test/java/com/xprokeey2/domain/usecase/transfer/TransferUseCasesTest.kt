package com.xprokeey2.domain.usecase.transfer

import com.xprokeey2.data.crypto.VaultCryptoImpl
import com.xprokeey2.domain.model.ExportFormat
import com.xprokeey2.domain.model.ImportFileProblem
import com.xprokeey2.domain.model.ImportRecord
import com.xprokeey2.domain.model.PickedFile
import com.xprokeey2.domain.repository.UserFileRepository
import com.xprokeey2.domain.repository.VaultTransferRepository
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferUseCasesTest {

    private val crypto = VaultCryptoImpl()

    private class FakeVaultSession(override var vaultKey: String?) : VaultSession {
        override fun unlock(vaultKey: String) {
            this.vaultKey = vaultKey
        }

        override fun lock() {
            vaultKey = null
        }
    }

    private class FakeTransfer(private val download: Resource<ByteArray> = Resource.Success(ByteArray(0))) : VaultTransferRepository {
        var imported: List<ImportRecord>? = null

        override suspend fun download(format: ExportFormat) = download

        override suspend fun import(records: List<ImportRecord>): Resource<Int> {
            imported = records
            return Resource.Success(records.size)
        }
    }

    private class FakeFiles(private val content: String = "", private val canWrite: Boolean = true) : UserFileRepository {
        var written: ByteArray? = null
        val deleted = mutableListOf<String>()
        var reads = 0

        override suspend fun describe(uri: String) = PickedFile(uri, "file", content.length.toLong())

        override suspend fun read(uri: String): Resource<ByteArray> {
            reads++
            return Resource.Success(content.encodeToByteArray())
        }

        override suspend fun write(uri: String, bytes: ByteArray): Resource<Unit> {
            if (!canWrite) return Resource.Error(DataError.Unknown("disk full"))
            written = bytes
            return Resource.Success(Unit)
        }

        override suspend fun delete(uri: String) {
            deleted += uri
        }
    }

    private fun file(name: String) = PickedFile("content://picked", name, 100)

    @Test
    fun importEncryptsCsvPasswordsWithTheVaultKey() = runBlocking {
        val transfer = FakeTransfer()
        val files = FakeFiles("name,username,password,url,notes,category\n\"Github\",\"Vanshgoel2610\",\"ckd,)w[nv\",\"https://github.com\",\"\",\"Personal\"\n")

        val result = ImportVaultUseCase(transfer, files, crypto, FakeVaultSession(VAULT_KEY))(file("XproKey.csv"))

        assertEquals(Resource.Success(1), result)
        val record = transfer.imported!!.single()
        assertEquals("Github", record.title)
        assertTrue(record.password != "ckd,)w[nv")
        assertEquals("ckd,)w[nv", crypto.decryptWithVaultKey(record.password, VAULT_KEY))
    }

    @Test
    fun importSendsXpkPasswordsAsTheyAre() = runBlocking {
        val encrypted = crypto.encryptWithVaultKey("wall", VAULT_KEY)
        val csv = "name,username,password,url,notes,category\n\"AppLock\",\"Vansh\",\"$encrypted\",\"https://applock.com\",\"\",\"Travel\"\n"
        val xpk = """{"format": "xprokey-encrypted-v1", "data": "${java.util.Base64.getEncoder().encodeToString(csv.encodeToByteArray())}"}"""
        val transfer = FakeTransfer()

        ImportVaultUseCase(transfer, FakeFiles(xpk), crypto, FakeVaultSession(VAULT_KEY))(file("XproKey.xpk"))

        assertEquals(encrypted, transfer.imported!!.single().password)
    }

    @Test
    fun importReadsFilesWithAByteOrderMark() = runBlocking {
        val transfer = FakeTransfer()
        ImportVaultUseCase(transfer, FakeFiles("﻿[{\"name\": \"Mail\", \"password\": \"p\"}]"), crypto, FakeVaultSession(VAULT_KEY))(file("a.json"))
        assertEquals("Mail", transfer.imported!!.single().title)
    }

    @Test
    fun importNeedsTheVaultAndAKnownFileType() = runBlocking {
        val files = FakeFiles("x")
        val locked = ImportVaultUseCase(FakeTransfer(), files, crypto, FakeVaultSession(null))(file("a.csv"))
        assertEquals(Resource.Error(DataError.VaultLocked), locked)
        assertEquals(0, files.reads)

        val transfer = FakeTransfer()
        val unsupported = ImportVaultUseCase(transfer, FakeFiles("x"), crypto, FakeVaultSession(VAULT_KEY))(file("a.txt"))
        assertEquals(Resource.Error(DataError.ImportFile(ImportFileProblem.UNSUPPORTED_TYPE)), unsupported)
        assertNull(transfer.imported)
    }

    @Test
    fun csvExportIsDecryptedAndUnreadablePasswordsAreEmpty() = runBlocking {
        val encrypted = crypto.encryptWithVaultKey("ckd,)w[nv", VAULT_KEY)
        val serverCsv = "name,username,password,url,notes,category\n" +
            "\"Github\",\"Vanshgoel2610\",\"$encrypted\",\"https://github.com\",\"\",\"Personal\"\n" +
            "\"gmail\",\"test@gmail.com\",\"ENCRYPTED_TEST_STRING_123456\",\"https://gmail.com\",\"test account\",\"Work\"\n"
        val files = FakeFiles()

        val result = ExportVaultUseCase(FakeTransfer(Resource.Success(serverCsv.encodeToByteArray())), files, crypto, FakeVaultSession(VAULT_KEY))(
            ExportFormat.CSV,
            "content://saved",
        )

        assertEquals(Resource.Success(Unit), result)
        assertEquals(
            "name,username,password,url,notes,category\n" +
                "\"Github\",\"Vanshgoel2610\",\"ckd,)w[nv\",\"https://github.com\",\"\",\"Personal\"\n" +
                "\"gmail\",\"test@gmail.com\",\"\",\"https://gmail.com\",\"test account\",\"Work\"\n",
            files.written!!.decodeToString(),
        )
        assertTrue(files.deleted.isEmpty())
    }

    @Test
    fun xpkExportIsSavedAsTheServerSentIt() = runBlocking {
        val xpk = """{"format": "xprokey-encrypted-v1", "data": "bmFtZQo="}""".encodeToByteArray()
        val files = FakeFiles()
        ExportVaultUseCase(FakeTransfer(Resource.Success(xpk)), files, crypto, FakeVaultSession(VAULT_KEY))(ExportFormat.ENCRYPTED, "content://saved")
        assertArrayEquals(xpk, files.written)
    }

    @Test
    fun failedExportRemovesTheNewFile() = runBlocking {
        val offline = FakeFiles()
        val noInternet = ExportVaultUseCase(FakeTransfer(Resource.Error(DataError.NoInternet)), offline, crypto, FakeVaultSession(VAULT_KEY))(
            ExportFormat.JSON,
            "content://saved",
        )
        assertEquals(Resource.Error(DataError.NoInternet), noInternet)
        assertEquals(listOf("content://saved"), offline.deleted)

        val full = FakeFiles(canWrite = false)
        ExportVaultUseCase(FakeTransfer(Resource.Success(ByteArray(3))), full, crypto, FakeVaultSession(VAULT_KEY))(ExportFormat.ENCRYPTED, "content://saved")
        assertEquals(listOf("content://saved"), full.deleted)

        val broken = FakeFiles()
        val badJson = ExportVaultUseCase(FakeTransfer(Resource.Success("{oops".encodeToByteArray())), broken, crypto, FakeVaultSession(VAULT_KEY))(
            ExportFormat.JSON,
            "content://saved",
        )
        assertTrue(badJson is Resource.Error)
        assertEquals(listOf("content://saved"), broken.deleted)

        val locked = FakeFiles()
        val lockedResult = ExportVaultUseCase(FakeTransfer(), locked, crypto, FakeVaultSession(null))(ExportFormat.CSV, "content://saved")
        assertEquals(Resource.Error(DataError.VaultLocked), lockedResult)
        assertEquals(listOf("content://saved"), locked.deleted)
    }

    private companion object {
        const val VAULT_KEY = "TQkzi8jV90LIkYuRaWsDS+2eg5H290vBdeLpW2GgCv8="
    }
}
