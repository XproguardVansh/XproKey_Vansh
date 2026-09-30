package com.xprokeey2.domain.model

/** Settings > Security: how long the app may go untouched before the [TimeoutAction] happens. */
enum class TimeoutDuration(val value: String, val minutes: Int?) {
    ONE_MINUTE("1", 1),
    FIVE_MINUTES("5", 5),
    FIFTEEN_MINUTES("15", 15),
    THIRTY_MINUTES("30", 30),
    ONE_HOUR("60", 60),
    FOUR_HOURS("240", 240),
    NEVER("never", null);

    companion object {
        /** The server's value ("1"…"240" or "never"); null when missing or unknown. */
        fun of(value: String?): TimeoutDuration? = entries.firstOrNull { it.value == value }
    }
}

enum class TimeoutAction(val value: String) {
    /** Clears the session and the vault key (the web's "Recommended" option). */
    LOGOUT("logout"),

    /** Keeps the session; the master password unlocks the vault again. */
    LOCK("lock");

    companion object {
        fun of(value: String?): TimeoutAction? = entries.firstOrNull { it.value == value }
    }
}

/** The web's defaults: never time out, and log out when it does. */
data class SessionTimeoutSettings(
    val duration: TimeoutDuration = TimeoutDuration.NEVER,
    val action: TimeoutAction = TimeoutAction.LOGOUT,
)

/** GET /me/security as the server sent it: either value may be missing. */
data class ServerSessionTimeout(
    val duration: TimeoutDuration?,
    val action: TimeoutAction?,
)
