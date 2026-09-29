package com.xprokeey2.domain.usecase.transfer

import com.xprokeey2.domain.model.ImportFileProblem
import com.xprokeey2.domain.model.ImportRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The web's export/import file rules, checked against the user's real files. */
class VaultFileFormatTest {

    @Test
    fun cleanCategoryLikeTheWeb() {
        assertEquals("Others", VaultFileFormat.cleanCategory(""))
        assertEquals("Others", VaultFileFormat.cleanCategory(null))
        assertEquals("Work", VaultFileFormat.cleanCategory("Work"))
        assertEquals("Travel", VaultFileFormat.cleanCategory("  Travel "))
        assertEquals("Work", VaultFileFormat.cleanCategory("12 Work"))
        assertEquals("Others", VaultFileFormat.cleanCategory("12"))
        assertEquals("Others", VaultFileFormat.cleanCategory("   "))
    }

    @Test
    fun csvKeepsCommasQuotesAndLineBreaksInsideQuotes() {
        val rows = VaultFileFormat.parseCsv("a,b\r\n\"ckd,)w[nv\",\"say \"\"hi\"\"\",\"two\nlines\"\nlast,")
        assertEquals(listOf("a", "b"), rows[0])
        assertEquals(listOf("ckd,)w[nv", "say \"hi\"", "two\nlines"), rows[1])
        assertEquals(listOf("last", ""), rows[2])
        assertEquals(3, rows.size)
        assertEquals("\"ckd,)w[nv\",\"say \"\"hi\"\"\"", VaultFileFormat.csvLine(listOf("ckd,)w[nv", "say \"hi\"")))
    }

    @Test
    fun csvExportDecryptsPasswordsLikeTheWeb() = runBlocking {
        val exported = VaultFileFormat.decryptCsvExport(SERVER_CSV) { encrypted -> PLAIN[encrypted].orEmpty() }
        assertEquals(USER_CSV, exported)
    }

    @Test
    fun jsonExportDecryptsPasswordsLikeTheWeb() = runBlocking {
        val exported = VaultFileFormat.decryptJsonExport(SERVER_JSON) { encrypted -> PLAIN[encrypted].orEmpty() }
        assertEquals(USER_JSON, exported)
    }

    @Test
    fun importsTheUsersCsvWithTheCommaInThePassword() {
        val parsed = VaultFileFormat.parseImport("XproKey.csv", USER_CSV)
        assertFalse(parsed.passwordsEncrypted)
        assertEquals(3, parsed.records.size)
        assertEquals(ImportRecord("Github", "Vanshgoel2610", "ckd,)w[nv", "https://github.com", "", "Personal"), parsed.records[0])
        assertEquals("VanshXpro\r", parsed.records[2].password)
    }

    @Test
    fun importsTheUsersJson() {
        val parsed = VaultFileFormat.parseImport("XproKey1.json", USER_JSON)
        assertFalse(parsed.passwordsEncrypted)
        assertEquals(ImportRecord("AppLock", "Vansh", "wall", "https://applock.com", "", "Travel"), parsed.records[1])
        assertEquals(3, parsed.records.size)
    }

    @Test
    fun importsTheUsersXpkWithPasswordsStillEncrypted() {
        val parsed = VaultFileFormat.parseImport("XproKey (1).xpk", USER_XPK)
        assertTrue(parsed.passwordsEncrypted)
        assertEquals(8, parsed.records.size)
        assertEquals(
            ImportRecord("Github", "Vanshgoel2610", "WMTsPB2mbrelVZ0p3HjRbw8h0l7Ld5N9zKQeG+X3ulLyc5rlEw==", "https://github.com", "", "Personal"),
            parsed.records[0],
        )
        assertEquals("Others", parsed.records[2].category)
    }

    @Test
    fun jsonRules() {
        // A plain array works too; items without name or title are skipped; title is used without a name.
        val parsed = VaultFileFormat.parseImport(
            "list.JSON",
            """[{"title": "Mail", "password": "p1", "category": "12 Work"}, {"username": "nobody"}, {"name": ""}]""",
        )
        assertEquals(listOf(ImportRecord("Mail", "", "p1", "", "", "Work")), parsed.records)
    }

    @Test
    fun windowsLineEndingsDontStickToThePassword() {
        val parsed = VaultFileFormat.parseImport("x.csv", "name,username,password\r\nVansh,vansh123,VanshXpro\r\n")
        assertEquals("VanshXpro", parsed.records.single().password)
        assertEquals("Others", parsed.records.single().category)
    }

    @Test
    fun rejectsWhatTheWebRejects() {
        assertProblem(ImportFileProblem.UNSUPPORTED_TYPE) { VaultFileFormat.parseImport("notes.txt", USER_CSV) }
        assertProblem(ImportFileProblem.INVALID_XPK) { VaultFileFormat.parseImport("a.xpk", """{"format": "other", "data": ""}""") }
        assertProblem(ImportFileProblem.NO_RECORDS) { VaultFileFormat.parseImport("a.csv", "name,username,password\n\n") }
        assertProblem(ImportFileProblem.UNREADABLE) { VaultFileFormat.parseImport("a.json", "{not json") }
        assertProblem(ImportFileProblem.UNREADABLE) { VaultFileFormat.parseImport("a.json", """{"exported_at": "x"}""") }
    }

    private fun assertProblem(expected: ImportFileProblem, block: () -> Unit) {
        try {
            block()
            fail("Expected $expected")
        } catch (e: ImportFileException) {
            assertEquals(expected, e.problem)
        }
    }

    private companion object {
        /** Ciphertext → password, standing in for the vault key. "POSTMAN" can't be decrypted. */
        val PLAIN = mapOf("CT1" to "ckd,)w[nv", "CT2" to "wall", "CT3" to "VanshXpro\r")

        const val SERVER_CSV = "name,username,password,url,notes,category\n" +
            "\"Github\",\"Vanshgoel2610\",\"CT1\",\"https://github.com\",\"\",\"Personal\"\n" +
            "\"AppLock\",\"Vansh\",\"CT2\",\"https://applock.com\",\"\",\"Travel\"\n" +
            "\"Vansh\",\"vansh123\",\"CT3\",\"\",\"\",\"Others\"\n"

        /** From the user's XproKey.csv (web export). */
        const val USER_CSV = "name,username,password,url,notes,category\n" +
            "\"Github\",\"Vanshgoel2610\",\"ckd,)w[nv\",\"https://github.com\",\"\",\"Personal\"\n" +
            "\"AppLock\",\"Vansh\",\"wall\",\"https://applock.com\",\"\",\"Travel\"\n" +
            "\"Vansh\",\"vansh123\",\"VanshXpro\r\",\"\",\"\",\"Others\"\n"

        val SERVER_JSON = """
            {"exported_at": "2026-09-29T09:31:09Z", "items": [
              {"name": "Github", "username": "Vanshgoel2610", "password": "CT1", "url": "https://github.com", "notes": "", "category": "Personal"},
              {"name": "AppLock", "username": "Vansh", "password": "CT2", "url": "https://applock.com", "notes": "", "category": "Travel"},
              {"name": "Vansh", "username": "vansh123", "password": "CT3", "url": "", "notes": "", "category": ""}
            ]}
        """.trimIndent()

        /** From the user's XproKey1.json (web export), first three items. */
        val USER_JSON = """
            {
              "exported_at": "2026-09-29T09:31:09Z",
              "items": [
                {
                  "name": "Github",
                  "username": "Vanshgoel2610",
                  "password": "ckd,)w[nv",
                  "url": "https://github.com",
                  "notes": "",
                  "category": "Personal"
                },
                {
                  "name": "AppLock",
                  "username": "Vansh",
                  "password": "wall",
                  "url": "https://applock.com",
                  "notes": "",
                  "category": "Travel"
                },
                {
                  "name": "Vansh",
                  "username": "vansh123",
                  "password": "VanshXpro\r",
                  "url": "",
                  "notes": "",
                  "category": "Others"
                }
              ]
            }
        """.trimIndent()

        /** The user's XproKey.xpk (web "Download .xpk"). */
        val USER_XPK = """
            {
              "format": "xprokey-encrypted-v1",
              "exported_at": "2026-09-29T09:31:17Z",
              "hint": "Passwords are encrypted. Import back into Xprokey to restore.",
              "data": "bmFtZSx1c2VybmFtZSxwYXNzd29yZCx1cmwsbm90ZXMsY2F0ZWdvcnkKIkdpdGh1YiIsIlZhbnNoZ29lbDI2MTAiLCJXTVRzUEIybWJyZWxWWjBwM0hqUmJ3OGgwbDdMZDVOOXpLUWVHK1gzdWxMeWM1cmxFdz09IiwiaHR0cHM6Ly9naXRodWIuY29tIiwiIiwiUGVyc29uYWwiCiJBcHBMb2NrIiwiVmFuc2giLCJaSkpITWFNek1XTlY3TEZiYjV3S0x1ZlBwQ1NibXpjSUQ3R1FwellxZnpvPSIsImh0dHBzOi8vYXBwbG9jay5jb20iLCIiLCJUcmF2ZWwiCiJWYW5zaCIsInZhbnNoMTIzIiwidUtqTUp0YXN5Q3k3NlpzTmhHT2Rvd2tOaFQweEEybGFYcTFRTTJaU3FEM0R0TkgvczdrPSIsIiIsIiIsIk90aGVycyIKIktyaXNobmExIiwia3Jpc2huYTEyMyIsInJkdEp3ZGMyejNTQ25LZ1hQNWI4Nm00TnlMUWNuTWZOc0N1SU9qZERpT1I0OW1EMnl5ajhFQT09IiwiaHR0cHM6Ly9qYWouY29tIiwiIiwiT3RoZXJzIgoiS3Jpc2huYSIsImtyaXNobmExMjMiLCJyZHRKd2RjMnozU0NuS2dYUDViODZtNE55TFFjbk1mTnNDdUlPamREaU9SNDltRDJ5eWo4RUE9PSIsIiIsIiIsIk90aGVycyIKIlZhbnNoIiwidmFuc2gxMjMiLCJ1S2pNSnRhc3lDeTc2WnNOaEdPZG93a05oVDB4QTJsYVhxMVFNMlpTcUQzRHROSC9zN2s9IiwiIiwiIiwiT3RoZXJzIgoiQXBwTG9jayIsIlZhbnNoIiwiWkpKSE1hTXpNV05WN0xGYmI1d0tMdWZQcENTYm16Y0lEN0dRcHpZcWZ6bz0iLCJodHRwczovL2FwcGxvY2suY29tIiwiIiwiVHJhdmVsIgoiR2l0aHViIiwiVmFuc2hnb2VsMjYxMCIsIldNVHNQQjJtYnJlbFZaMHAzSGpSYnc4aDBsN0xkNU45ektRZUcrWDN1bEx5YzVybEV3PT0iLCJodHRwczovL2dpdGh1Yi5jb20iLCIiLCJQZXJzb25hbCIK"
            }
        """.trimIndent()
    }
}
