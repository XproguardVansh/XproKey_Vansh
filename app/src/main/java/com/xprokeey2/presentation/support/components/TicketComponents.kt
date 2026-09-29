package com.xprokeey2.presentation.support.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprokeey2.R
import com.xprokeey2.domain.model.TicketCategory
import com.xprokeey2.domain.model.TicketPriority
import com.xprokeey2.domain.model.TicketStatus
import com.xprokeey2.presentation.theme.XpTheme

/** Tailwind colours of the web pills: [base] (-500) tints the background and border. */
private class PillColors(val base: Color, val text: Color, val darkText: Color)

private val Blue = PillColors(Color(0xFF3B82F6), Color(0xFF2563EB), Color(0xFF60A5FA))
private val Amber = PillColors(Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFFBBF24))
private val Purple = PillColors(Color(0xFFA855F7), Color(0xFF9333EA), Color(0xFFC084FC))
private val Rose = PillColors(Color(0xFFF43F5E), Color(0xFFE11D48), Color(0xFFFB7185))
private val Slate = PillColors(Color(0xFF64748B), Color(0xFF475569), Color(0xFF94A3B8))
private val Red = PillColors(Color(0xFFEF4444), Color(0xFFDC2626), Color(0xFFF87171))
private val Orange = PillColors(Color(0xFFF97316), Color(0xFFEA580C), Color(0xFFFB923C))

/** "Technical", "Billing", … as the web list and details show them. */
@get:StringRes
val TicketCategory.label: Int
    get() = when (this) {
        TicketCategory.TECHNICAL -> R.string.support_cat_technical
        TicketCategory.BILLING -> R.string.support_cat_billing
        TicketCategory.ACCOUNT -> R.string.support_cat_account
        TicketCategory.BUG_REPORT -> R.string.support_cat_bug
        TicketCategory.GENERAL -> R.string.support_cat_general
    }

/** "Technical Issue", "Billing & Payment", … as the web form's category menu shows them. */
@get:StringRes
val TicketCategory.formLabel: Int
    get() = when (this) {
        TicketCategory.TECHNICAL -> R.string.ticket_cat_technical
        TicketCategory.BILLING -> R.string.ticket_cat_billing
        TicketCategory.ACCOUNT -> R.string.ticket_cat_account
        TicketCategory.BUG_REPORT -> R.string.ticket_cat_bug
        TicketCategory.GENERAL -> R.string.ticket_cat_general
    }

@get:StringRes
val TicketStatus.label: Int
    get() = when (this) {
        TicketStatus.OPEN -> R.string.support_open
        TicketStatus.IN_PROGRESS -> R.string.support_in_progress
        TicketStatus.RESOLVED -> R.string.support_resolved
        TicketStatus.CLOSED -> R.string.support_closed
    }

@DrawableRes
private fun TicketCategory.icon(): Int = when (this) {
    TicketCategory.TECHNICAL -> R.drawable.ic_wrench
    TicketCategory.BILLING -> R.drawable.ic_credit_card
    TicketCategory.ACCOUNT -> R.drawable.ic_user
    TicketCategory.BUG_REPORT -> R.drawable.ic_bug
    TicketCategory.GENERAL -> R.drawable.ic_help_circle
}

private fun TicketCategory.colors(): PillColors = when (this) {
    TicketCategory.TECHNICAL -> Blue
    TicketCategory.BILLING -> Amber
    TicketCategory.ACCOUNT -> Purple
    TicketCategory.BUG_REPORT -> Rose
    TicketCategory.GENERAL -> Slate
}

/** TICK-0014 in the web's blue mono pill; with [onCopy] it shows a copy icon and copies on tap. */
@Composable
fun TicketIdPill(displayId: String, modifier: Modifier = Modifier, onCopy: (() -> Unit)? = null) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.08f))
            .border(1.dp, colors.primary.copy(alpha = 0.18f), shape)
            .then(
                if (onCopy != null) {
                    Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.cd_copy_ticket_id), onClick = onCopy)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = displayId,
            style = XpTheme.typography.mono.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp, fontWeight = FontWeight.Bold),
            color = colors.primary,
        )
        if (onCopy != null) {
            Spacer(Modifier.width(6.dp))
            Icon(
                painter = painterResource(R.drawable.ic_copy),
                contentDescription = null,
                tint = colors.textLabel,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/** Category with its icon: Technical (blue), Billing (amber), Account (purple), Bug Report (rose), General. */
@Composable
fun TicketCategoryPill(category: String) {
    val value = TicketCategory.of(category)
    Pill(text = stringResource(value.label), colors = value.colors(), icon = value.icon())
}

/** "Medium Priority" etc.: urgent red, high orange, low grey, medium blue. */
@Composable
fun TicketPriorityPill(priority: String) {
    val (label, colors) = when (TicketPriority.of(priority)) {
        TicketPriority.URGENT -> R.string.support_priority_urgent to Red
        TicketPriority.HIGH -> R.string.support_priority_high to Orange
        TicketPriority.LOW -> R.string.support_priority_low to Slate
        TicketPriority.MEDIUM -> R.string.support_priority_medium to Blue
    }
    Pill(text = stringResource(label), colors = colors, icon = null)
}

@Composable
private fun Pill(text: String, colors: PillColors, @DrawableRes icon: Int?) {
    val isDark = XpTheme.colors.isDark
    val textColor = if (isDark) colors.darkText else colors.text
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(colors.base.copy(alpha = if (isDark) 0.16f else 0.08f))
            .border(1.dp, colors.base.copy(alpha = if (isDark) 0.35f else 0.25f), shape)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = textColor, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            style = XpTheme.typography.bodyBold.copy(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
            color = textColor,
        )
    }
}

/** Filled status pill with a white dot: Open amber, In Progress blue, Resolved green, Closed grey. */
@Composable
fun TicketStatusPill(status: String, modifier: Modifier = Modifier, large: Boolean = false) {
    val value = TicketStatus.of(status)
    val background = when (value) {
        TicketStatus.OPEN -> Color(0xFFF59E0B)
        TicketStatus.IN_PROGRESS -> Color(0xFF3B82F6)
        TicketStatus.RESOLVED -> Color(0xFF059669)
        TicketStatus.CLOSED -> Color(0xFF64748B)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = if (large) 14.dp else 12.dp, vertical = if (large) 6.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(if (large) 8.dp else 6.dp)
                .background(Color.White, CircleShape),
        )
        Spacer(Modifier.width(if (large) 8.dp else 6.dp))
        Text(
            text = stringResource(value.label).uppercase(),
            style = XpTheme.typography.caption.copy(fontSize = 11.sp, letterSpacing = 0.8.sp),
            color = Color.White,
        )
    }
}
