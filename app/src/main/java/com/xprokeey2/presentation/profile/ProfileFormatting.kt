package com.xprokeey2.presentation.profile

import com.xprokeey2.domain.model.AccountProfile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val JoinedDateFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)
private val ValidTillFormat = DateTimeFormatter.ofPattern("d/M/yyyy", Locale.US)

/** The web's name: the account's name, else the part of the email before "@". */
fun AccountProfile.fullName(): String = name.ifBlank { email.substringBefore('@') }

/** The web Profile's initials: first letters of the first two words, else the first two letters. */
fun profileInitials(fullName: String, email: String): String {
    val words = fullName.trim().split(" ").filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> email.take(1).uppercase().ifEmpty { "U" }
        words.size >= 2 -> (words[0].take(1) + words[1].take(1)).uppercase()
        else -> words[0].take(2).uppercase()
    }
}

/** The web's `toLocaleDateString("en-US", { month: "long", day: "numeric", year: "numeric" })`; today if unknown, like the web. */
fun joinedDate(joinedAt: Instant?, zone: ZoneId = ZoneId.systemDefault()): String =
    (joinedAt?.atZone(zone)?.toLocalDate() ?: LocalDate.now(zone)).format(JoinedDateFormat)

/** The web's `toLocaleDateString("en-IN")` for the license, e.g. "26/9/2027". */
fun validTillDate(expiresAt: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    expiresAt.atZone(zone).toLocalDate().format(ValidTillFormat)
