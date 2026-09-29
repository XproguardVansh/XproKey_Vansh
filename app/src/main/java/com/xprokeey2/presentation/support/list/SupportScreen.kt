package com.xprokeey2.presentation.support.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.model.SupportTicket
import com.xprokeey2.domain.model.TicketCategory
import com.xprokeey2.domain.model.TicketStatus
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.components.XpTextField
import com.xprokeey2.presentation.passwords.components.dateTime
import com.xprokeey2.presentation.support.components.TicketCategoryPill
import com.xprokeey2.presentation.support.components.TicketIdPill
import com.xprokeey2.presentation.support.components.TicketPriorityPill
import com.xprokeey2.presentation.support.components.TicketStatusPill
import com.xprokeey2.presentation.support.components.label
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import com.xprokeey2.presentation.util.ResultMessageEffect
import com.xprokeey2.presentation.workspace.CompactButton
import com.xprokeey2.presentation.workspace.CompactButtonStyle
import com.xprokeey2.presentation.workspace.IconTile
import com.xprokeey2.presentation.workspace.LoadingBlock
import com.xprokeey2.presentation.workspace.UserBadge
import com.xprokeey2.presentation.workspace.WorkspacePanel
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun SupportScreenRoot(
    resultMessage: String?,
    onResultMessageShown: () -> Unit,
    onSectionClick: (WorkspaceSection) -> Unit,
    onNewTicket: () -> Unit,
    onViewTicket: (ticketId: String) -> Unit,
    onSignInRequired: (message: String) -> Unit,
    viewModel: SupportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SupportEvent.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(event.message.asString(context)) }
            is SupportEvent.SignInRequired -> onSignInRequired(event.message.asString(context))
        }
    }
    ResultMessageEffect(resultMessage, snackbarHostState, onResultMessageShown)
    // Also after coming back from a ticket or the New Ticket form.
    LifecycleResumeEffect(Unit) {
        viewModel.onAction(SupportAction.Refresh)
        onPauseOrDispose { }
    }

    SupportScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onSectionClick = onSectionClick,
        onNewTicket = onNewTicket,
        onViewTicket = onViewTicket,
    )
}

@Composable
fun SupportScreen(
    state: SupportUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (SupportAction) -> Unit,
    onSectionClick: (WorkspaceSection) -> Unit,
    onNewTicket: () -> Unit,
    onViewTicket: (ticketId: String) -> Unit,
) {
    val colors = XpTheme.colors
    WorkspaceScaffold(
        user = state.user,
        currentSection = WorkspaceSection.SUPPORT,
        onSectionClick = onSectionClick,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.support_title),
                        style = XpTheme.typography.pageTitle,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    CompactButton(
                        text = stringResource(R.string.support_new_ticket),
                        icon = R.drawable.ic_plus,
                        onClick = onNewTicket,
                        style = CompactButtonStyle.Primary,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.support_subtitle),
                    style = XpTheme.typography.body.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(4.dp))
            }
            item { StatsGrid(state) }
            item { FilterPanel(state, onAction) }

            val visible = state.visibleTickets
            when {
                state.isLoading -> item { LoadingBlock() }
                state.loadError != null -> item {
                    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 24.dp) {
                        Text(
                            text = state.loadError.asString(),
                            style = XpTheme.typography.body.copy(fontSize = 13.sp, lineHeight = 19.sp),
                            color = colors.textSecondary,
                        )
                        Spacer(Modifier.height(16.dp))
                        XpPrimaryButton(
                            text = stringResource(R.string.retry),
                            onClick = { onAction(SupportAction.Refresh) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                visible.isEmpty() -> item { EmptyPanel(state.emptyState, onNewTicket) }
                else -> items(visible, key = { it.id }) { ticket ->
                    TicketCard(ticket = ticket, onClick = { onViewTicket(ticket.id) })
                }
            }
        }
    }
}

@Composable
private fun StatsGrid(state: SupportUiState) {
    val stats = state.stats
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(stringResource(R.string.support_total), stats.total, dot = null, Modifier.weight(1f))
            StatTile(stringResource(R.string.support_open), stats.open, dot = OpenColor, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(stringResource(R.string.support_in_progress), stats.inProgress, dot = InProgressColor, Modifier.weight(1f))
            StatTile(stringResource(R.string.support_resolved), stats.resolved, dot = ResolvedColor, Modifier.weight(1f))
        }
    }
}

private val OpenColor = Color(0xFFF59E0B)
private val InProgressColor = Color(0xFF3B82F6)
private val ResolvedColor = Color(0xFF10B981)

/** Web stat card: the label (with a coloured dot for a status) above the count. */
@Composable
private fun StatTile(label: String, value: Int, dot: Color?, modifier: Modifier) {
    val colors = XpTheme.colors
    WorkspacePanel(modifier = modifier, contentPadding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (dot != null) {
                Box(Modifier.size(8.dp).background(dot, CircleShape))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label.uppercase(),
                style = XpTheme.typography.fieldLabel.copy(fontSize = 10.5.sp, letterSpacing = 0.8.sp),
                color = dot ?: colors.textLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(text = value.toString(), style = XpTheme.typography.statValue.copy(fontSize = 24.sp), color = colors.textPrimary)
    }
}

/** Status tabs, then the search box and the category menu. */
@Composable
private fun FilterPanel(state: SupportUiState, onAction: (SupportAction) -> Unit) {
    val colors = XpTheme.colors
    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatusTab(stringResource(R.string.support_all), state.statusFilter == null) { onAction(SupportAction.StatusSelected(null)) }
            TicketStatus.entries.forEach { status ->
                StatusTab(stringResource(status.label), state.statusFilter == status) { onAction(SupportAction.StatusSelected(status)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            XpTextField(
                value = state.query,
                onValueChange = { onAction(SupportAction.QueryChanged(it)) },
                placeholder = stringResource(R.string.support_search),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = colors.textPlaceholder,
                        modifier = Modifier.size(16.dp),
                    )
                },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            CategoryFilter(selected = state.categoryFilter, onSelected = { onAction(SupportAction.CategorySelected(it)) })
        }
    }
}

@Composable
private fun StatusTab(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = XpTheme.colors
    Text(
        text = text,
        style = XpTheme.typography.bodyBold.copy(fontSize = 12.5.sp),
        color = if (isSelected) colors.onPrimary else colors.textSecondary,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) colors.primary else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/** "All Categories" menu of the web list. */
@Composable
private fun CategoryFilter(selected: TicketCategory?, onSelected: (TicketCategory?) -> Unit) {
    val colors = XpTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(11.dp)
    val label = selected?.let { stringResource(it.label) } ?: stringResource(R.string.support_all_categories)
    Box {
        Row(
            modifier = Modifier
                .width(140.dp)
                .height(48.dp)
                .clip(shape)
                .background(colors.fieldBackground)
                .border(1.dp, if (expanded) colors.primary else colors.fieldBorder, shape)
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = XpTheme.typography.fieldText.copy(fontSize = 12.5.sp),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = colors.textLabel,
                modifier = Modifier.size(15.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = colors.surface) {
            val options = listOf<TicketCategory?>(null) + TicketCategory.entries
            options.forEach { category ->
                val isSelected = category == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            text = category?.let { stringResource(it.label) } ?: stringResource(R.string.support_all_categories),
                            style = XpTheme.typography.fieldText.copy(
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = colors.textPrimary,
                        )
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onSelected(category)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TicketCard(ticket: SupportTicket, onClick: () -> Unit) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.divider, shape)
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TicketIdPill(ticket.displayId)
            TicketCategoryPill(ticket.category)
            TicketPriorityPill(ticket.priority)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = ticket.subject,
            style = XpTheme.typography.bodyBold.copy(fontSize = 15.sp, lineHeight = 20.sp),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = ticket.message,
            style = XpTheme.typography.body.copy(fontSize = 12.5.sp),
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = null,
                tint = colors.textLabel,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.support_created, ticket.createdAt?.dateTime() ?: stringResource(R.string.just_now)),
                style = XpTheme.typography.body.copy(fontSize = 11.5.sp),
                color = colors.textLabel,
            )
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = colors.divider)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TicketStatusPill(ticket.status)
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.support_view_details),
                style = XpTheme.typography.bodyBold.copy(fontSize = 12.5.sp),
                color = colors.primary,
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun EmptyPanel(emptyState: SupportEmptyState, onNewTicket: () -> Unit) {
    val colors = XpTheme.colors
    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 28.dp) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconTile(icon = R.drawable.ic_message_square, tint = colors.primary, size = 52.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(emptyState.title),
                style = XpTheme.typography.sectionTitle,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(emptyState.body),
                style = XpTheme.typography.body.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            CompactButton(
                text = stringResource(emptyState.button),
                icon = R.drawable.ic_plus,
                onClick = onNewTicket,
                style = CompactButtonStyle.Primary,
            )
        }
    }
}

private val PreviewTickets = listOf(
    SupportTicket(
        id = "14", ticketNumber = null, name = "Vansh", email = "goelv2610@gmail.com", category = "general",
        subject = "Testing", message = "Testing", priority = "medium", status = "open",
        createdAt = Instant.parse("2026-09-29T11:33:00Z"), updatedAt = null,
    ),
)

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun SupportScreenPreview() {
    XproKeyTheme(darkTheme = true) {
        SupportScreen(
            state = SupportUiState(
                user = UserBadge.from("Vansh Goel", "goelv2610@gmail.com"),
                isLoading = false,
                tickets = PreviewTickets,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}, onSectionClick = {}, onNewTicket = {}, onViewTicket = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun SupportScreenEmptyPreview() {
    XproKeyTheme(darkTheme = false) {
        SupportScreen(
            state = SupportUiState(isLoading = false, statusFilter = TicketStatus.IN_PROGRESS, tickets = PreviewTickets),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}, onSectionClick = {}, onNewTicket = {}, onViewTicket = {},
        )
    }
}
