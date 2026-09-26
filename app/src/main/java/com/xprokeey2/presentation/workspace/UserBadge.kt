package com.xprokeey2.presentation.workspace

import com.xprokeey2.domain.model.User

/** Avatar + name in the top bar and drawer. Like the web app, it is built from the email. */
data class UserBadge(
    val initials: String,
    val displayName: String,
    val email: String,
) {
    companion object {
        /** "goelvansh770@gmail.com" → "GO" / "goelvansh770", as the web header shows it. */
        fun from(name: String, email: String): UserBadge {
            val handle = email.substringBefore('@').ifBlank { name }
            return UserBadge(
                initials = handle.filter(Char::isLetterOrDigit).take(2).uppercase().ifEmpty { "?" },
                displayName = handle,
                email = email,
            )
        }
    }
}

fun User.toBadge() = UserBadge.from(name = name, email = email)
