package com.xprokeey2.presentation.passwords.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xprokeey2.R
import com.xprokeey2.presentation.theme.XpTheme
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Letter avatar colours of the web Passwords list (V in green, K in blue). */
private val AvatarColors = listOf(
    Color(0xFF6366F1), Color(0xFFEC4899), Color(0xFFF59E0B), Color(0xFF0EA5E9),
    Color(0xFF8B5CF6), Color(0xFFEF4444), Color(0xFF10B981), Color(0xFF14B8A6),
)

/** Web dashboard letter colours: blue-600, indigo-600, purple-600, slate-800, sky-500. */
private val RecentItemColors = listOf(
    Color(0xFF2563EB), Color(0xFF4F46E5), Color(0xFF9333EA), Color(0xFF1E293B), Color(0xFF0EA5E9),
)

private val SchemeAndWww = Regex("^(https?://)?(www\\.)?")
private val Whitespace = Regex("\\s+")

/**
 * Web `ItemIcon`: the item's logo from Google's favicon service. The domain comes from the URL or,
 * for items without one, from a few known names; null (show the letter) without a dotted domain.
 */
fun faviconUrl(url: String, name: String): String? {
    var domain = ""
    if (url.isNotEmpty()) {
        val cleanedUrl = url.trim().lowercase()
        val withHttp = if (cleanedUrl.startsWith("http")) cleanedUrl else "https://$cleanedUrl"
        domain = runCatching { URI(withHttp).host }.getOrNull()
            ?: url.replace(SchemeAndWww, "").split("/")[0]
    }
    if (domain.isEmpty() && name.isNotEmpty()) {
        val cleanName = name.trim().lowercase().replace(Whitespace, "")
        domain = when {
            "github" in cleanName -> "github.com"
            "gmail" in cleanName -> "gmail.com"
            "google" in cleanName -> "google.com"
            "netflix" in cleanName -> "netflix.com"
            "aws" in cleanName || "amazon" in cleanName -> "amazon.com"
            "figma" in cleanName -> "figma.com"
            else -> ""
        }
    }
    return if ("." in domain) "https://www.google.com/s2/favicons?sz=64&domain=$domain" else null
}

/**
 * An item's icon in the Passwords list and details: its site logo on a white tile, or its letter.
 * [softLetter] (password cards): a light tint with the letter in a deeper shade of the same colour.
 */
@Composable
fun VaultItemAvatar(
    title: String,
    url: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    softLetter: Boolean = false,
) {
    val shape = RoundedCornerShape(size * 0.3f)
    val letter = title.trim().firstOrNull()?.uppercaseChar() ?: '?'
    val hue = AvatarColors[letter.code % AvatarColors.size]
    val isDark = XpTheme.colors.isDark
    SiteLogo(
        logoUrl = remember(url, title) { faviconUrl(url, title) },
        size = size,
        shape = shape,
        tileColor = Color.White,
        modifier = modifier,
    ) {
        LetterTile(
            letter = letter.toString(),
            color = if (softLetter) hue.copy(alpha = if (isDark) 0.2f else 0.14f) else hue,
            letterColor = when {
                !softLetter -> Color.White
                isDark -> lerp(hue, Color.White, 0.35f)
                else -> lerp(hue, Color.Black, 0.3f)
            },
            shape = shape,
            fontSize = (size.value * 0.4f).sp,
        )
    }
}

/** Dashboard "Recently updated": the web's `ItemIcon` (a 36dp circle with its own letter colours). */
@Composable
fun RecentItemIcon(title: String, url: String, modifier: Modifier = Modifier) {
    // `item.title ? item.title.charAt(0).toUpperCase() : "V"`
    val initial = title.firstOrNull()?.uppercase() ?: "V"
    SiteLogo(
        logoUrl = remember(url, title) { faviconUrl(url, title) },
        size = 36.dp,
        shape = CircleShape,
        tileColor = XpTheme.colors.surface,
        modifier = modifier,
    ) {
        LetterTile(
            letter = initial,
            color = RecentItemColors[initial[0].code % RecentItemColors.size],
            letterColor = Color.White,
            shape = CircleShape,
            fontSize = 14.sp,
        )
    }
}

/** The logo on a bordered tile; [fallback] when there is none or it fails to load (web `onError`). */
@Composable
private fun SiteLogo(
    logoUrl: String?,
    size: Dp,
    shape: Shape,
    tileColor: Color,
    modifier: Modifier,
    fallback: @Composable () -> Unit,
) {
    var failed by remember(logoUrl) { mutableStateOf(false) }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (logoUrl == null || failed) {
            fallback()
        } else {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onError = { failed = true },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(tileColor)
                    .border(1.dp, XpTheme.colors.divider, shape)
                    .padding(size / 6),
            )
        }
    }
}

@Composable
private fun LetterTile(letter: String, color: Color, letterColor: Color, shape: Shape, fontSize: TextUnit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = XpTheme.typography.bodyBold.copy(fontSize = fontSize),
            color = letterColor,
        )
    }
}

/** Category pill; the built-in categories get their own colour, custom ones are neutral. */
@Composable
fun CategoryChip(category: String, modifier: Modifier = Modifier) {
    if (category.isBlank()) return
    val color = when (category.lowercase()) {
        "personal" -> Color(0xFF3B82F6)
        "work" -> Color(0xFF8B5CF6)
        "finance" -> Color(0xFF10B981)
        "shopping" -> Color(0xFFEC4899)
        "travel" -> Color(0xFF14B8A6)
        "social" -> Color(0xFFF59E0B)
        else -> XpTheme.colors.textSecondary
    }
    Text(
        text = category,
        style = XpTheme.typography.bodyBold.copy(fontSize = 11.sp, lineHeight = 14.sp),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** "github.com ↗" (or [label] ↗, e.g. "Visit") — opens the item's website in the browser. */
@Composable
fun WebsiteLink(
    url: String,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = displayDomain(url),
    showIcon: Boolean = true,
) {
    val colors = XpTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button) { onOpen(url) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = XpTheme.typography.body.copy(fontSize = 12.sp),
            color = colors.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (showIcon) {
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.ic_external_link),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(11.dp),
            )
        }
    }
}

/** "https://www.github.com/login" (or "github.com/login") → "github.com". */
fun displayDomain(url: String): String {
    val address = url.trim()
    val host = runCatching { URI(withScheme(address)).host }.getOrNull()
    return host?.removePrefix("www.")?.takeIf { it.isNotBlank() } ?: address
}

/** Opens [url] in the browser; false when no app can open it. */
fun openLink(uriHandler: UriHandler, url: String): Boolean =
    runCatching { uriHandler.openUri(withScheme(url.trim())) }.isSuccess

/** Like the web: an address that doesn't start with "http" is opened as https. */
private fun withScheme(address: String): String =
    if (address.startsWith("http", ignoreCase = true)) address else "https://$address"

private val ShortDate = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
private val DateTime = DateTimeFormatter.ofPattern("MMM d, yyyy, hh:mm a", Locale.getDefault())

/** "Sep 28, 2026" in the phone's time zone. */
fun Instant?.shortDate(): String = this?.atZone(ZoneId.systemDefault())?.format(ShortDate).orEmpty()

/** "Sep 28, 2026, 03:48 PM" in the phone's time zone. */
fun Instant?.dateTime(): String = this?.atZone(ZoneId.systemDefault())?.format(DateTime).orEmpty()
