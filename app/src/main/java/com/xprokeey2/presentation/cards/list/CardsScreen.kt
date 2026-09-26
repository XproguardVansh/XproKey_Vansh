package com.xprokeey2.presentation.cards.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.model.Card
import com.xprokeey2.domain.model.CardBrand
import com.xprokeey2.presentation.cards.components.PaymentCardView
import com.xprokeey2.presentation.cards.components.formatExpiry
import com.xprokeey2.presentation.cards.components.maskedCardNumber
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.components.XpTextField
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import com.xprokeey2.presentation.workspace.CompactButton
import com.xprokeey2.presentation.workspace.CompactButtonStyle
import com.xprokeey2.presentation.workspace.LoadingBlock
import com.xprokeey2.presentation.workspace.PageHeader
import com.xprokeey2.presentation.workspace.UserBadge
import com.xprokeey2.presentation.workspace.WorkspacePanel
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import kotlinx.coroutines.launch

@Composable
fun CardsScreenRoot(
    resultMessage: String?,
    onResultMessageShown: () -> Unit,
    onSectionClick: (WorkspaceSection) -> Unit,
    onAddCard: () -> Unit,
    onViewCard: (cardId: Long) -> Unit,
    onSignInRequired: (message: String) -> Unit,
    viewModel: CardsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CardsEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            is CardsEvent.SignInRequired -> onSignInRequired(event.message.asString(context))
        }
    }
    // e.g. "Card saved successfully." after coming back from Add card.
    LaunchedEffect(resultMessage) {
        if (resultMessage != null) {
            onResultMessageShown()
            snackbarHostState.showSnackbar(resultMessage)
        }
    }
    LifecycleResumeEffect(Unit) {
        viewModel.onAction(CardsAction.Refresh)
        onPauseOrDispose { }
    }

    CardsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onSectionClick = onSectionClick,
        onAddCard = onAddCard,
        onViewCard = onViewCard,
    )
}

@Composable
fun CardsScreen(
    state: CardsUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (CardsAction) -> Unit,
    onSectionClick: (WorkspaceSection) -> Unit,
    onAddCard: () -> Unit,
    onViewCard: (cardId: Long) -> Unit,
) {
    WorkspaceScaffold(
        user = state.user,
        currentSection = WorkspaceSection.CARDS,
        onSectionClick = onSectionClick,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                PageHeader(
                    title = stringResource(R.string.cards_title),
                    action = {
                        CompactButton(
                            text = stringResource(R.string.add_card),
                            icon = R.drawable.ic_plus,
                            onClick = onAddCard,
                            style = CompactButtonStyle.Primary,
                        )
                    },
                )
            }

            when {
                state.isLoading -> item { LoadingBlock() }
                state.loadError != null -> item {
                    MessagePanel(
                        title = stringResource(R.string.cards_load_failed),
                        body = state.loadError.asString(),
                        buttonText = stringResource(R.string.retry),
                        onButtonClick = { onAction(CardsAction.Refresh) },
                    )
                }
                state.cards.isEmpty() -> item {
                    MessagePanel(
                        title = stringResource(R.string.cards_empty_title),
                        body = stringResource(R.string.cards_empty_body),
                        buttonText = "+ " + stringResource(R.string.add_card),
                        onButtonClick = onAddCard,
                    )
                }
                else -> {
                    item {
                        XpTextField(
                            value = state.query,
                            onValueChange = { onAction(CardsAction.QueryChanged(it)) },
                            placeholder = stringResource(R.string.search_cards),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_search),
                                    contentDescription = null,
                                    tint = XpTheme.colors.textPlaceholder,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    val visible = state.visibleCards
                    if (visible.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.cards_no_match, state.query.trim()),
                                style = XpTheme.typography.body.copy(fontSize = 13.sp),
                                color = XpTheme.colors.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                            )
                        }
                    }
                    items(visible, key = { it.id }) { card ->
                        CardListItem(
                            card = card,
                            revealedNumber = state.revealedNumbers[card.id],
                            isRevealing = state.revealingCardId == card.id,
                            onToggleReveal = { onAction(CardsAction.ToggleReveal(card.id)) },
                            onView = { onViewCard(card.id) },
                            onDelete = { onAction(CardsAction.DeleteClicked(card)) },
                        )
                    }
                }
            }
        }
    }

    state.cardPendingDelete?.let { card ->
        DeleteCardDialog(
            card = card,
            isDeleting = state.isDeleting,
            onConfirm = { onAction(CardsAction.DeleteConfirmed) },
            onDismiss = { onAction(CardsAction.DeleteDismissed) },
        )
    }
}

@Composable
private fun CardListItem(
    card: Card,
    revealedNumber: String?,
    isRevealing: Boolean,
    onToggleReveal: () -> Unit,
    onView: () -> Unit,
    onDelete: () -> Unit,
) {
    Column {
        PaymentCardView(
            label = card.label,
            brand = card.brand,
            number = revealedNumber ?: maskedCardNumber(card.last4),
            holderName = card.holderName,
            expiry = formatExpiry(card.expiryMonth, card.expiryYear),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactButton(
                text = stringResource(if (revealedNumber != null) R.string.hide else R.string.reveal),
                icon = if (revealedNumber != null) R.drawable.ic_eye_off else R.drawable.ic_eye,
                onClick = onToggleReveal,
                isLoading = isRevealing,
                modifier = Modifier.weight(1f),
            )
            CompactButton(
                text = stringResource(R.string.view),
                icon = R.drawable.ic_credit_card,
                onClick = onView,
                modifier = Modifier.weight(1f),
            )
            CompactButton(
                text = null,
                icon = R.drawable.ic_trash,
                onClick = onDelete,
                contentDescription = stringResource(R.string.cd_delete_card),
            )
        }
    }
}

@Composable
private fun MessagePanel(title: String, body: String, buttonText: String, onButtonClick: () -> Unit) {
    val colors = XpTheme.colors
    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 28.dp) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.ic_credit_card),
                contentDescription = null,
                tint = colors.textLabel,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = XpTheme.typography.sectionTitle,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = body,
                style = XpTheme.typography.body.copy(fontSize = 13.sp, lineHeight = 19.sp),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            XpPrimaryButton(text = buttonText, onClick = onButtonClick)
        }
    }
}

@Composable
fun DeleteCardDialog(
    card: Card,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = XpTheme.colors
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        containerColor = colors.surface,
        title = {
            Text(
                text = stringResource(R.string.delete_card_title),
                style = XpTheme.typography.dialogTitle,
                color = colors.textPrimary,
            )
        },
        text = {
            Text(
                text = stringResource(
                    R.string.delete_card_body,
                    card.label.ifBlank { stringResource(R.string.card_fallback_name) },
                    card.last4,
                ),
                style = XpTheme.typography.dialogBody,
                color = colors.textSecondary,
            )
        },
        confirmButton = {
            CompactButton(
                text = stringResource(R.string.delete),
                icon = R.drawable.ic_trash,
                onClick = onConfirm,
                style = CompactButtonStyle.Danger,
                isLoading = isDeleting,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = XpTheme.typography.buttonSecondary,
                    color = colors.textSecondary,
                )
            }
        },
    )
}

private val PreviewCards = listOf(
    Card(267, "Personal", "Vansh Goel", "debit", CardBrand.RUPAY, "2086", "BOB", "", 7, 2028),
    Card(270, "Visa Test", "Shreyash Jadhav", "credit", CardBrand.VISA, "1111", "", "Visa test card", 12, 2030),
)

@Preview(name = "List", showBackground = true, heightDp = 900)
@Composable
private fun CardsScreenPreview() {
    XproKeyTheme(darkTheme = true) {
        CardsScreen(
            state = CardsUiState(
                user = UserBadge.from("Vansh Goel", "goelvansh770@gmail.com"),
                isLoading = false,
                cards = PreviewCards,
                revealedNumbers = mapOf(270L to "4111 1111 1111 1111"),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onSectionClick = {},
            onAddCard = {},
            onViewCard = {},
        )
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun CardsScreenEmptyPreview() {
    XproKeyTheme(darkTheme = false) {
        CardsScreen(
            state = CardsUiState(isLoading = false, loadError = null),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onSectionClick = {},
            onAddCard = {},
            onViewCard = {},
        )
    }
}
