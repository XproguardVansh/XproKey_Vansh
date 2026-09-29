package com.xprokeey2.domain.usecase.transfer

import com.xprokeey2.domain.model.ImportFileProblem
import com.xprokeey2.domain.model.ImportRecord
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.util.Base64

/** A file the web would reject too; [problem] says why. */
class ImportFileException(val problem: ImportFileProblem) : Exception(problem.name)

/** Records read from an import file. An .xpk's passwords are vault-key ciphertext already. */
data class ParsedImportFile(val records: List<ImportRecord>, val passwordsEncrypted: Boolean)

/**
 * The web's Tools > Export / Import file rules (app/(app)/tools/export and import). One difference,
 * chosen by the user: CSV values may contain commas, quotes and line breaks. The web splits on every
 * comma and drops all quotes.
 */
object VaultFileFormat {

    const val XPK_FORMAT = "xprokey-encrypted-v1"

    private val LeadingNumber = Regex("""\d+\s+(.*)""")
    private val DigitsOnly = Regex("""\d+""")

    /** `JSON.stringify(parsed, null, 2)` */
    @OptIn(ExperimentalSerializationApi::class)
    private val PrettyJson = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    /** Web `cleanCategory`: empty → "Others", "12 Work" → "Work", "12" → "Others". */
    fun cleanCategory(category: String?): String {
        if (category.isNullOrEmpty()) return "Others"
        val cat = category.trim()
        LeadingNumber.matchEntire(cat)?.let { return it.groupValues[1].ifEmpty { "Others" } }
        if (DigitsOnly.matches(cat)) return "Others"
        return cat.ifEmpty { "Others" }
    }

    /**
     * CSV export: the server's header stays; in every row the password is decrypted ([decrypt]
     * gives "" when it can't, like the web) and the category cleaned.
     */
    suspend fun decryptCsvExport(csv: String, decrypt: suspend (String) -> String): String {
        val rows = parseCsv(csv)
        val out = StringBuilder(rows.firstOrNull()?.joinToString(",").orEmpty()).append('\n')
        for (row in dataRows(rows)) {
            val values = listOf(row.at(0), row.at(1), decrypt(row.at(2)), row.at(3), row.at(4), cleanCategory(row.at(5)))
            out.append(csvLine(values)).append('\n')
        }
        return out.toString()
    }

    /**
     * JSON export: every item's password decrypted and its category cleaned (missing → "Others"),
     * the rest untouched, printed with 2-space indents.
     */
    suspend fun decryptJsonExport(json: String, decrypt: suspend (String) -> String): String {
        val root = Json.parseToJsonElement(json).jsonObject
        val items = root.getValue("items").jsonArray.map { element ->
            val item = element.jsonObject
            val category = item["category"].text()
            val changes = mapOf(
                "password" to JsonPrimitive(decrypt(item["password"].text().orEmpty())),
                "category" to JsonPrimitive(if (category.isNullOrEmpty()) "Others" else cleanCategory(category)),
            )
            JsonObject(item + changes)
        }
        return PrettyJson.encodeToString(JsonElement.serializer(), JsonObject(root + ("items" to JsonArray(items))))
    }

    /** Reads an import file by its extension (.csv, .json or .xpk), like the web. */
    fun parseImport(fileName: String, text: String): ParsedImportFile {
        val parsed = when (fileName.substringAfterLast('.').lowercase()) {
            "csv" -> ParsedImportFile(csvRecords(text), passwordsEncrypted = false)
            "json" -> ParsedImportFile(jsonRecords(text), passwordsEncrypted = false)
            "xpk" -> ParsedImportFile(csvRecords(xpkCsv(text)), passwordsEncrypted = true)
            else -> throw ImportFileException(ImportFileProblem.UNSUPPORTED_TYPE)
        }
        if (parsed.records.isEmpty()) throw ImportFileException(ImportFileProblem.NO_RECORDS)
        return parsed
    }

    /** CSV lines: "quoted" values may hold commas and line breaks, and "" stands for a quote. */
    fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var pending = false

        fun endRow() {
            row += field.toString()
            field.setLength(0)
            rows += row
            row = mutableListOf()
            pending = false
        }

        var i = 0
        while (i < text.length) {
            val c = text[i]
            pending = true
            when {
                inQuotes -> when {
                    c == '"' && text.getOrNull(i + 1) == '"' -> {
                        field.append('"')
                        i++
                    }
                    c == '"' -> inQuotes = false
                    else -> field.append(c)
                }
                c == '"' && field.isEmpty() -> inQuotes = true
                c == ',' -> {
                    row += field.toString()
                    field.setLength(0)
                }
                c == '\n' -> endRow()
                // "\r\n": the \n ends the line.
                c == '\r' -> if (text.getOrNull(i + 1) != '\n') endRow()
                else -> field.append(c)
            }
            i++
        }
        if (pending) endRow()
        return rows
    }

    /** One CSV line with every value quoted, like the server's and the web's files. */
    fun csvLine(values: List<String>): String =
        values.joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" }

    /** name, username, password, url, notes, category after the header line; empty lines skipped. */
    private fun csvRecords(text: String): List<ImportRecord> = dataRows(parseCsv(text)).map { row ->
        ImportRecord(
            title = row.at(0),
            username = row.at(1),
            password = row.at(2),
            url = row.at(3),
            notes = row.at(4),
            category = cleanCategory(row.at(5)),
        )
    }

    /** `parsed.items ?? parsed`; items without a name or title are skipped. */
    private fun jsonRecords(text: String): List<ImportRecord> {
        val items = when (val root = parseJson(text)) {
            is JsonArray -> root
            is JsonObject -> root["items"] as? JsonArray
            else -> null
        } ?: throw ImportFileException(ImportFileProblem.UNREADABLE)

        return items.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            if (!item["name"].isTruthy() && !item["title"].isTruthy()) return@mapNotNull null
            ImportRecord(
                // `item.name ?? item.title ?? ""`
                title = item["name"].value() ?: item["title"].value().orEmpty(),
                username = item["username"].value().orEmpty(),
                password = item["password"].value().orEmpty(),
                url = item["url"].value().orEmpty(),
                notes = item["notes"].value().orEmpty(),
                category = cleanCategory(item["category"].value().orEmpty()),
            )
        }
    }

    /** The CSV inside an .xpk: `{"format": "xprokey-encrypted-v1", "data": base64(CSV)}`. */
    private fun xpkCsv(text: String): String {
        val root = parseJson(text) as? JsonObject
        if (root?.get("format").value() != XPK_FORMAT) throw ImportFileException(ImportFileProblem.INVALID_XPK)
        val data = root?.get("data").value() ?: throw ImportFileException(ImportFileProblem.UNREADABLE)
        return try {
            // atob ignores whitespace in the Base64 text.
            Base64.getDecoder().decode(data.filterNot { it.isWhitespace() }).decodeToString(throwOnInvalidSequence = true)
        } catch (e: IllegalArgumentException) {
            throw ImportFileException(ImportFileProblem.UNREADABLE)
        } catch (e: CharacterCodingException) {
            throw ImportFileException(ImportFileProblem.UNREADABLE)
        }
    }

    private fun parseJson(text: String): JsonElement = try {
        Json.parseToJsonElement(text)
    } catch (e: SerializationException) {
        throw ImportFileException(ImportFileProblem.UNREADABLE)
    }

    private fun dataRows(rows: List<List<String>>) = rows.drop(1).filterNot { row -> row.all { it.isBlank() } }

    private fun List<String>.at(index: Int) = getOrElse(index) { "" }

    /** A string or number as text; null when missing or JSON null. */
    private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content

    /** Like [text], but objects and arrays are kept as JSON text (the web would send them as they are). */
    private fun JsonElement?.value(): String? = when (this) {
        null, JsonNull -> null
        is JsonPrimitive -> content
        else -> toString()
    }

    /** JavaScript truthiness of a JSON value. */
    private fun JsonElement?.isTruthy(): Boolean = when (this) {
        null, JsonNull -> false
        is JsonPrimitive -> if (isString) {
            content.isNotEmpty()
        } else {
            content != "false" && content.toDoubleOrNull().let { it == null || (it != 0.0 && !it.isNaN()) }
        }
        else -> true
    }
}
