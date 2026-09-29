package com.xprokeey2.domain.model

/** Tools > Export: the three files of the web app, saved under the web's file names. */
enum class ExportFormat(val fileName: String, val mimeType: String) {
    /** Passwords decrypted to plain text. */
    CSV("XproKey.csv", "text/csv"),

    /** Passwords decrypted to plain text. */
    JSON("XproKey.json", "application/json"),

    /** The server's .xpk backup as it is: passwords stay encrypted. */
    ENCRYPTED("XproKey.xpk", "application/octet-stream"),
}

/** One password as POST /import takes it; [password] is already encrypted with the vault key. */
data class ImportRecord(
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val category: String,
)

/** A file the user picked on the Import screen ([uri] is its content URI). */
data class PickedFile(
    val uri: String,
    val name: String,
    val sizeBytes: Long,
)

/** Why an import file was rejected; each has the web's message. */
enum class ImportFileProblem {
    /** Not .csv, .json or .xpk. */
    UNSUPPORTED_TYPE,

    /** An .xpk whose `format` isn't "xprokey-encrypted-v1". */
    INVALID_XPK,

    /** Nothing to import in the file. */
    NO_RECORDS,

    /** The file couldn't be read or parsed. */
    UNREADABLE,
}
