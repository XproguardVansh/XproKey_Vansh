package com.xprokeey2.domain.util

sealed interface DataError {
    /** The server rejected the request, e.g. `{"error": "Invalid email or password"}`. */
    data class Server(val code: Int, val message: String) : DataError

    /** Login attempted before the email was verified via OTP. */
    data class AccountNotVerified(val message: String) : DataError

    /** Login succeeded but the vault key couldn't be decrypted with the master password. */
    data object VaultUnlockFailed : DataError

    /** The recovery key entered during a password reset doesn't open this account's vault. */
    data object InvalidRecoveryKey : DataError

    /** No valid login session (token missing or rejected with 401): the user must sign in again. */
    data object SessionExpired : DataError

    data object NoInternet : DataError
    data object Timeout : DataError
    data class Unknown(val message: String? = null) : DataError
}
