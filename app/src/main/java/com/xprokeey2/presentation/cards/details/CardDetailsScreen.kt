package com.xprokeey2.presentation.cards.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.model.Card
import com.xprokeey2.domain.model.CardBrand
import com.xprokeey2.domain.model.CardCategory
import com.xprokeey2.domain.model.CardDetails
import com.xprokeey2.presentation.cards.components.PaymentCardView
import com.xprokeey2.presentation.cards.components.formatCardNumber
import com.xprokeey2.presentation.cards.components.formatExpiry
import com.xprokeey2.presentation.cards.components.labelRes
import com.xprokeey2.presentation.cards.components.maskedCardNumber
import com.xprokeey2.presentation.cards.list.DeleteCardDialog
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import com.xprokeey2.presentation.util.copyToClipboard
import com.xprokeey2.presentation.workspace.CompactButton
import com.xprokeey2.presentation.workspace.CompactButtonStyle
import com.xprokeey2.presentation.workspace.LoadingBlock
import com.xprokeey2.presentation.workspace.WorkspacePanel
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import kotlinx.coroutines.launch

@Composable
fun CardDetailsScreenRoot(
    resultMessage: String?,
    onResultMessageShown: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: (message: String) -> Unit,
    onSignInRequired: (message: String) -> Unit,
    viewModel: CardDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CardDetailsEvent.CopyToClipboard -> {
                val label = event.label.asString(context)
                copyToClipboard(context, label = label, value = event.value.asString(context), sensitive = event.sensitive)
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.copied, label)) }
            }
            is CardDetailsEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            is CardDetailsEvent.Deleted -> onDeleted(event.message.asString(context))
            is CardDetailsEvent.SignInRequired -> onSignInRequired(event.message.asString(context))
        }
    }
    // e.g. "Card updated successfully." after coming back from Edit.
    LaunchedEffect(resultMessage) {
        if (resultMessage != null) {
            onResultMessageShown()
            snackbarHostState.showSnackbar(resultMessage)
        }
    }
    LifecycleResumeEffect(Unit) {
        viewModel.onAction(CardDetailsAction.Refresh)
        onPauseOrDispose { }
    }

    CardDetailsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onEdit = onEdit,
    )
}

@Composable
fun CardDetailsScreen(
    state: CardDetailsUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (CardDetailsAction) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = XpTheme.colors
    WorkspaceScaffold(
        user = null,
        currentSection = WorkspaceSection.CARDS,
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
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.card_details_title),
                style = XpTheme.typography.pageTitle,
                color = colors.textPrimary,
            )

            val details = state.details
            when {
                state.isLoading -> LoadingBlock()
                details == null -> {
                    Spacer(Modifier.height(20.dp))
                    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 24.dp) {
                        Text(
                            text = state.loadError?.asString().orEmpty(),
                            style = XpTheme.typography.body.copy(fontSize = 13.sp, lineHeight = 19.sp),
                            color = colors.textSecondary,
                        )
                        Spacer(Modifier.height(16.dp))
                        XpPrimaryButton(
                            text = stringResource(R.string.retry),
                            onClick = { onAction(CardDetailsAction.Refresh) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                else -> CardDetailsContent(state = state, details = details, onAction = onAction, onEdit = onEdit)
            }
        }
    }

    val card = state.details?.card
    if (state.isDeleteDialogVisible && card != null) {
        DeleteCardDialog(
            card = card,
            isDeleting = state.isDeleting,
            onConfirm = { onAction(CardDetailsAction.DeleteConfirmed) },
            onDismiss = { onAction(CardDetailsAction.DeleteDismissed) },
        )
    }
}

@Composable
private fun CardDetailsContent(
    state: CardDetailsUiState,
    details: CardDetails,
    onAction: (CardDetailsAction) -> Unit,
    onEdit: () -> Unit,
) {
    val colors = XpTheme.colors
    val card = details.card
    val masked = maskedCardNumber(card.last4)
    val number = details.number
        ?.takeIf { state.isNumberRevealed }
        ?.let { formatCardNumber(it, card.brand) }
        ?: masked
    val expiry = formatExpiry(card.expiryMonth, card.expiryYear)
    val empty = stringResource(R.string.empty_value)
    val monoStyle = XpTheme.typography.mono.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 1.1.sp)

    Spacer(Modifier.height(20.dp))
    Text(
        text = stringResource(R.string.card_preview).uppercase(),
        style = XpTheme.typography.fieldLabel.copy(letterSpacing = 0.6.sp),
        color = colors.textLabel,
    )
    Spacer(Modifier.height(10.dp))
    PaymentCardView(
        label = card.label,
        brand = card.brand,
        number = number,
        holderName = card.holderName,
        expiry = expiry,
    )

    Spacer(Modifier.height(24.dp))
    WorkspacePanel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.card_details_section),
                style = XpTheme.typography.sectionTitle.copy(fontSize = 16.sp),
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            CompactButton(
                text = stringResource(R.string.edit),
                icon = null,
                onClick = onEdit,
            )
            Spacer(Modifier.width(8.dp))
            CompactButton(
                text = stringResource(R.string.delete),
                icon = R.drawable.ic_trash,
                onClick = { onAction(CardDetailsAction.DeleteClicked) },
                style = CompactButtonStyle.Danger,
            )
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = colors.divider)
        Spacer(Modifier.height(18.dp))

        val copy = { field: CardField -> onAction(CardDetailsAction.Copy(field)) }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DetailField(stringResource(R.string.label_card_type), card.label.ifBlank { empty }, onCopy = { copy(CardField.LABEL) })
            DetailField(stringResource(R.string.label_cardholder_name), card.holderName.ifBlank { empty }, onCopy = { copy(CardField.HOLDER_NAME) })
            DetailField(
                label = stringResource(R.string.label_card_number),
                value = number,
                valueStyle = monoStyle,
                revealed = state.isNumberRevealed,
                onToggleReveal = { onAction(CardDetailsAction.ToggleNumber) },
                onCopy = { copy(CardField.NUMBER) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailField(
                    label = stringResource(R.string.label_brand),
                    value = card.brand.displayName.ifBlank { empty },
                    onCopy = { copy(CardField.BRAND) },
                    modifier = Modifier.weight(1f),
                )
                DetailField(
                    label = stringResource(R.string.label_expiry_date),
                    value = expiry.replace("/", " / ").ifBlank { empty },
                    valueStyle = monoStyle,
                    onCopy = { copy(CardField.EXPIRY) },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailField(
                    label = stringResource(R.string.label_cvc),
                    value = details.cvc?.takeIf { state.isCvcRevealed } ?: "•••",
                    valueStyle = monoStyle,
                    revealed = state.isCvcRevealed,
                    onToggleReveal = { onAction(CardDetailsAction.ToggleCvc) },
                    onCopy = { copy(CardField.CVC) },
                    modifier = Modifier.weight(1f),
                )
                DetailField(
                    label = stringResource(R.string.label_card_category),
                    value = CardCategory.fromCardType(card.category)?.let { stringResource(it.labelRes) }
                        ?: card.category.ifBlank { empty },
                    onCopy = { copy(CardField.CATEGORY) },
                    modifier = Modifier.weight(1f),
                )
            }
            DetailField(stringResource(R.string.label_bank_name), card.bankName.ifBlank { empty }, onCopy = { copy(CardField.BANK_NAME) })
            DetailField(stringResource(R.string.label_notes), card.notes.ifBlank { empty }, onCopy = { copy(CardField.NOTES) })
        }
    }
}

/** Uppercase label with REVEAL / COPY on the right, value in a read-only box (Figma "Card details"). */
@Composable
private fun DetailField(
    label: String,
    value: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = XpTheme.typography.fieldText.copy(fontSize = 13.5.sp),
    revealed: Boolean = false,
    onToggleReveal: (() -> Unit)? = null,
) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(11.dp)
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label.uppercase(),
                style = XpTheme.typography.fieldLabel,
                color = colors.textLabel,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            if (onToggleReveal != null) {
                FieldAction(
                    text = stringResource(if (revealed) R.string.hide_action else R.string.reveal_action),
                    color = colors.textLabel,
                    onClick = onToggleReveal,
                )
                Spacer(Modifier.width(4.dp))
            }
            FieldAction(text = stringResource(R.string.copy_action), color = colors.primary, onClick = onCopy)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = valueStyle,
            color = colors.textPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.fieldBackground)
                .border(1.dp, colors.fieldBorder, shape)
                .padding(horizontal = 14.dp, vertical = 13.dp),
        )
    }
}

@Composable
private fun FieldAction(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text = text,
        style = XpTheme.typography.mono.copy(fontSize = 9.5.sp, lineHeight = 12.sp, letterSpacing = 0.3.sp),
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
    )
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun CardDetailsScreenPreview() {
    XproKeyTheme(darkTheme = false) {
        CardDetailsScreen(
            state = CardDetailsUiState(
                isLoading = false,
                isNumberRevealed = true,
                details = CardDetails(
                    card = Card(267, "Personal", "Vansh Goel", "debit", CardBrand.RUPAY, "2086", "BOB", "", 7, 2028),
                    number = "6522814342012086",
                    cvc = "123",
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onBack = {},
            onEdit = {},
        )
    }
}
