package com.xprokeey2.domain.model

import java.time.Instant

/** GET /me: who is signed in, and a Business account's license. */
data class AccountProfile(
    val email: String,
    /** Empty when the account has no name. */
    val name: String,
    val accountType: String?,
    val joinedAt: Instant?,
    val organizationName: String?,
    val licenseStatus: String?,
    val licenseExpiresAt: Instant?,
) {
    val isBusiness: Boolean get() = accountType == ACCOUNT_BUSINESS
    val isPersonal: Boolean get() = accountType == AccountSummary.ACCOUNT_PERSONAL

    companion object {
        const val ACCOUNT_BUSINESS = "business"
    }
}

/** The Profile page: the account, plus billing for a Personal account (null when it couldn't be read). */
data class ProfileOverview(
    val profile: AccountProfile,
    val billing: Billing?,
)
