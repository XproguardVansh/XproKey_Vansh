package com.xprokeey2.domain.model

/** Why the server's access guard refused a request (HTTP 403 with `next_action`). */
enum class AccessBlock {
    /** Personal account without an active subscription, or past its billing date. */
    PAYMENT,

    /** The free trial ended. */
    TRIAL_EXPIRED,

    /** Business account without an active license. */
    ACTIVATE_LICENSE;

    companion object {
        fun of(nextAction: String?): AccessBlock? = when (nextAction) {
            "payment" -> PAYMENT
            "trial_expired" -> TRIAL_EXPIRED
            "activate_license" -> ACTIVATE_LICENSE
            else -> null
        }
    }
}
