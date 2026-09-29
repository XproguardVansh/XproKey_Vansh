package com.xprokeey2.presentation.support.details

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.model.SupportTicket
import com.xprokeey2.domain.model.TicketStatus
import com.xprokeey2.presentation.passwords.components.dateTime
import com.xprokeey2.presentation.support.components.TicketCategoryPill
import com.xprokeey2.presentation.support.components.TicketIdPill
import com.xprokeey2.presentation.support.components.TicketPriorityPill
import com.xprokeey2.presentation.support.components.TicketStatusPill
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import com.xprokeey2.presentation.util.ResultMessageEffect
import com.xprokeey2.presentation.util.copyToClipboard
import com.xprokeey2.presentation.workspace.IconTile
import com.xprokeey2.presentation.workspace.LoadingBlock
import com.xprokeey2.presentation.workspace.WorkspacePanel
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun SupportTicketScreenRoot(
    resultMessage: String?,
    onResultMessageShown: () -> Unit,
    onBack: () -> Unit,
    onLoadFailed: (message: String) -> Unit,
    onSignInRequired: (message: String) -> Unit,
    viewModel: SupportTicketViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SupportTicketEvent.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(event.message.asString(context)) }
            is SupportTicketEvent.CopyToClipboard -> {
                copyToClipboard(context, label = context.getString(R.string.cd_copy_ticket_id), value = event.value, sensitive = false)
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.ticket_id_copied)) }
            }
            is SupportTicketEvent.LoadFailed -> onLoadFailed(event.message.asString(context))
            is SupportTicketEvent.SignInRequired -> onSignInRequired(event.message.asString(context))
        }
    }
    // e.g. "Support ticket created successfully!" right after sending it.
    ResultMessageEffect(resultMessage, snackbarHostState, onResultMessageShown)

    SupportTicketScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBack = onBack,
    )
}

@Composable
fun SupportTicketScreen(
    state: SupportTicketUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (SupportTicketAction) -> Unit,
    onBack: () -> Unit,
) {
    WorkspaceScaffold(
        user = null,
        currentSection = WorkspaceSection.SUPPORT,
        onSectionClick = {},
        snackbarHostState = snackbarHostState,
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            val ticket = state.ticket
            if (state.isLoading || ticket == null) {
                LoadingBlock()
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    RefreshButton(isRefreshing = state.isRefreshing, onClick = { onAction(SupportTicketAction.Refresh) })
                }
                Spacer(Modifier.height(14.dp))
                TicketSummary(ticket = ticket, onCopyId = { onAction(SupportTicketAction.CopyTicketId) })
                Spacer(Modifier.height(16.dp))
                IssueDescription(message = ticket.message)
                Spacer(Modifier.height(16.dp))
                SecurityNote()
            }
        }
    }
}

/** Outlined "Refresh" button; its icon spins while the ticket reloads. */
@Composable
private fun RefreshButton(isRefreshing: Boolean, onClick: () -> Unit) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val angle = if (isRefreshing) {
        val transition = rememberInfiniteTransition(label = "refresh")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
            label = "refreshAngle",
        ).value
    } else {
        0f
    }
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(shape)
            .border(1.dp, colors.fieldBorder, shape)
            .clickable(enabled = !isRefreshing, role = Role.Button, onClick = onClick)
            .alpha(if (isRefreshing) 0.7f else 1f)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_refresh),
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier
                .size(14.dp)
                .rotate(angle),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.ticket_refresh),
            style = XpTheme.typography.bodyBold.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TicketSummary(ticket: SupportTicket, onCopyId: () -> Unit) {
    val colors = XpTheme.colors
    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TicketIdPill(displayId = ticket.displayId, onCopy = onCopyId)
            TicketCategoryPill(ticket.category)
            TicketPriorityPill(ticket.priority)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = ticket.subject,
            style = XpTheme.typography.pageTitle.copy(fontSize = 22.sp, lineHeight = 28.sp),
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(12.dp))
        TicketStatusPill(status = ticket.status, large = true)

        Spacer(Modifier.height(16.dp))
        val bannerShape = RoundedCornerShape(16.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(bannerShape)
                .background(colors.fieldBackground)
                .border(1.dp, colors.divider, bannerShape)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon = R.drawable.ic_headphones, tint = colors.primary, size = 34.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(ticket.statusDescription()),
                    style = XpTheme.typography.bodyBold.copy(fontSize = 12.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.ticket_email_note),
                    style = XpTheme.typography.body.copy(fontSize = 11.sp, lineHeight = 15.sp),
                    color = colors.textLabel,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = colors.divider)
        Spacer(Modifier.height(16.dp))
        val notAvailable = stringResource(R.string.ticket_not_available)
        Row {
            MetaItem(stringResource(R.string.ticket_requester), ticket.name, bold = true, modifier = Modifier.weight(1f))
            MetaItem(stringResource(R.string.ticket_contact_email), ticket.email, bold = true, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Row {
            MetaItem(
                stringResource(R.string.ticket_submitted),
                ticket.createdAt?.dateTime() ?: notAvailable,
                bold = false,
                modifier = Modifier.weight(1f),
            )
            MetaItem(
                stringResource(R.string.ticket_last_updated),
                (ticket.updatedAt ?: ticket.createdAt)?.dateTime() ?: notAvailable,
                bold = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun SupportTicket.statusDescription(): Int = when (TicketStatus.of(status)) {
    TicketStatus.OPEN -> R.string.ticket_status_open_desc
    TicketStatus.IN_PROGRESS -> R.string.ticket_status_in_progress_desc
    TicketStatus.RESOLVED -> R.string.ticket_status_resolved_desc
    TicketStatus.CLOSED -> R.string.ticket_status_closed_desc
}

@Composable
private fun MetaItem(label: String, value: String, bold: Boolean, modifier: Modifier) {
    val colors = XpTheme.colors
    Column(modifier = modifier.padding(end = 8.dp)) {
        Text(
            text = label.uppercase(),
            style = XpTheme.typography.fieldLabel.copy(letterSpacing = 0.8.sp),
            color = colors.textLabel,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = if (bold) {
                XpTheme.typography.bodyBold.copy(fontSize = 12.5.sp)
            } else {
                XpTheme.typography.body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            },
            color = if (bold) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun IssueDescription(message: String) {
    val colors = XpTheme.colors
    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_file_text),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.ticket_issue_description),
                style = XpTheme.typography.bodyBold.copy(fontSize = 14.sp),
                color = colors.textPrimary,
            )
        }
        Spacer(Modifier.height(14.dp))
        val shape = RoundedCornerShape(16.dp)
        SelectionContainer {
            Text(
                text = message,
                style = XpTheme.typography.body.copy(fontSize = 13.5.sp, lineHeight = 21.sp),
                color = colors.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.fieldBackground)
                    .border(1.dp, colors.divider, shape)
                    .padding(18.dp),
            )
        }
    }
}

@Composable
private fun SecurityNote() {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.primary.copy(alpha = if (colors.isDark) 0.08f else 0.04f))
            .border(1.dp, colors.primary.copy(alpha = 0.14f), shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_shield),
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.ticket_security_note),
            style = XpTheme.typography.body.copy(fontSize = 12.sp, lineHeight = 17.sp),
            color = colors.textSecondary,
        )
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun SupportTicketScreenPreview() {
    val at = Instant.parse("2026-09-29T11:33:00Z")
    XproKeyTheme(darkTheme = true) {
        SupportTicketScreen(
            state = SupportTicketUiState(
                isLoading = false,
                ticket = SupportTicket(
                    id = "14", ticketNumber = null, name = "Vansh", email = "goelv2610@gmail.com", category = "general",
                    subject = "Testing", message = "Testing", priority = "medium", status = "open",
                    createdAt = at, updatedAt = at,
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onBack = {},
        )
    }
}
