package com.xprokeey2.presentation.passwords.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.model.PasswordStrength
import com.xprokeey2.presentation.components.PasswordStrengthMeter
import com.xprokeey2.presentation.components.XpCheckbox
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.components.XpSecondaryButton
import com.xprokeey2.presentation.components.XpTextField
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import com.xprokeey2.presentation.workspace.CompactButton
import com.xprokeey2.presentation.workspace.CompactButtonStyle
import com.xprokeey2.presentation.workspace.LoadingBlock
import com.xprokeey2.presentation.workspace.WorkspacePanel
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import kotlinx.coroutines.launch

@Composable
fun PasswordFormScreenRoot(
    onBack: () -> Unit,
    onSaved: (message: String) -> Unit,
    onSignInRequired: (message: String) -> Unit,
    viewModel: PasswordFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is PasswordFormEvent.Saved -> onSaved(event.message.asString(context))
            is PasswordFormEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            is PasswordFormEvent.SignInRequired -> onSignInRequired(event.message.asString(context))
        }
    }

    PasswordFormScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBack = onBack,
    )
}

@Composable
fun PasswordFormScreen(
    state: PasswordFormUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (PasswordFormAction) -> Unit,
    onBack: () -> Unit,
) {
    val colors = XpTheme.colors
    WorkspaceScaffold(
        user = null,
        currentSection = WorkspaceSection.PASSWORDS,
        onSectionClick = {},
        snackbarHostState = snackbarHostState,
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text(
                text = stringResource(if (state.isEditing) R.string.edit_password_title else R.string.add_password_title),
                style = XpTheme.typography.pageTitle,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(if (state.isEditing) R.string.edit_password_subtitle else R.string.add_password_subtitle),
                style = XpTheme.typography.body.copy(fontSize = 12.5.sp),
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(20.dp))

            when {
                state.isLoadingItem -> LoadingBlock()
                state.loadError != null -> WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 24.dp) {
                    Text(
                        text = state.loadError.asString(),
                        style = XpTheme.typography.body.copy(fontSize = 13.sp, lineHeight = 19.sp),
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(16.dp))
                    XpPrimaryButton(
                        text = stringResource(R.string.retry),
                        onClick = { onAction(PasswordFormAction.RetryLoad) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> PasswordFormFields(state = state, onAction = onAction, onCancel = onBack)
            }
        }
    }

    if (state.isNewCategoryDialogVisible) {
        NewCategoryDialog(state = state, onAction = onAction)
    }
}

@Composable
private fun PasswordFormFields(
    state: PasswordFormUiState,
    onAction: (PasswordFormAction) -> Unit,
    onCancel: () -> Unit,
) {
    val colors = XpTheme.colors
    val focusManager = LocalFocusManager.current
    val monoStyle = XpTheme.typography.mono.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.5.sp)

    WorkspacePanel(modifier = Modifier.fillMaxWidth()) {
        if (state.passwordUnreadable) {
            Text(
                text = stringResource(R.string.password_unreadable),
                style = XpTheme.typography.body.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                color = colors.warning,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.warning.copy(alpha = 0.10f))
                    .padding(12.dp),
            )
            Spacer(Modifier.height(18.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            XpTextField(
                value = state.title,
                onValueChange = { onAction(PasswordFormAction.TitleChanged(it)) },
                label = stringResource(R.string.label_name),
                required = true,
                placeholder = stringResource(R.string.placeholder_name),
                error = state.titleError?.asString(),
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            XpTextField(
                value = state.url,
                onValueChange = { onAction(PasswordFormAction.UrlChanged(it)) },
                label = stringResource(R.string.label_website_url),
                required = true,
                placeholder = stringResource(R.string.placeholder_website_url),
                error = state.urlError?.asString(),
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            XpTextField(
                value = state.username,
                onValueChange = { onAction(PasswordFormAction.UsernameChanged(it)) },
                label = stringResource(if (state.isEditing) R.string.label_username_email else R.string.label_username),
                required = true,
                placeholder = stringResource(R.string.placeholder_username),
                error = state.usernameError?.asString(),
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            CategoryPicker(
                selected = state.category,
                categories = state.categories,
                enabled = !state.isSaving,
                onSelected = { onAction(PasswordFormAction.CategorySelected(it)) },
                onNewCategory = { onAction(PasswordFormAction.NewCategoryClicked) },
            )
            Column {
                XpTextField(
                    value = state.password,
                    onValueChange = { onAction(PasswordFormAction.PasswordChanged(it)) },
                    label = stringResource(R.string.label_password),
                    required = true,
                    placeholder = "••••••••••••",
                    error = state.passwordError?.asString(),
                    enabled = !state.isSaving,
                    textStyle = monoStyle,
                    placeholderStyle = monoStyle,
                    visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = ImeAction.Next),
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable(role = Role.Button) { onAction(PasswordFormAction.TogglePasswordVisibility) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(if (state.isPasswordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye),
                                    contentDescription = stringResource(
                                        if (state.isPasswordVisible) R.string.cd_hide_password else R.string.cd_show_password
                                    ),
                                    tint = colors.textPlaceholder,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            CompactButton(
                                text = stringResource(R.string.generate),
                                icon = R.drawable.ic_wand,
                                onClick = { onAction(PasswordFormAction.GeneratePassword) },
                                style = CompactButtonStyle.Primary,
                                enabled = !state.isSaving,
                                modifier = Modifier.height(34.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                state.strength?.let { strength ->
                    Spacer(Modifier.height(8.dp))
                    PasswordStrengthMeter(strength = strength, modifier = Modifier.fillMaxWidth())
                }
            }
            XpTextField(
                value = state.notes,
                onValueChange = { onAction(PasswordFormAction.NotesChanged(it)) },
                label = stringResource(if (state.isEditing) R.string.label_secure_notes else R.string.label_notes),
                placeholder = stringResource(R.string.placeholder_password_notes),
                enabled = !state.isSaving,
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            XpCheckbox(
                checked = state.isFavorite,
                onCheckedChange = { onAction(PasswordFormAction.FavoriteChanged(it)) },
            ) {
                Text(
                    text = stringResource(R.string.mark_favorite),
                    style = XpTheme.typography.bodyBold.copy(fontSize = 13.sp),
                    color = colors.textPrimary,
                )
            }
        }
    }

    Spacer(Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        XpSecondaryButton(
            text = stringResource(R.string.cancel),
            onClick = onCancel,
            enabled = !state.isSaving,
            modifier = Modifier.weight(1f),
        )
        XpPrimaryButton(
            text = stringResource(if (state.isEditing) R.string.save_vault else R.string.save_password),
            onClick = {
                focusManager.clearFocus()
                onAction(PasswordFormAction.Save)
            },
            isLoading = state.isSaving,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Category dropdown; its last entry "+ New Category" opens a small dialog. */
@Composable
private fun CategoryPicker(
    selected: String,
    categories: List<String>,
    enabled: Boolean,
    onSelected: (String) -> Unit,
    onNewCategory: () -> Unit,
) {
    val colors = XpTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(11.dp)
    val options = if (selected in categories) categories else listOf(selected) + categories

    Column {
        Text(
            text = stringResource(R.string.label_category).uppercase(),
            style = XpTheme.typography.fieldLabel,
            color = colors.textLabel,
        )
        Spacer(Modifier.height(8.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(shape)
                    .background(colors.fieldBackground)
                    .border(1.dp, if (expanded) colors.primary else colors.fieldBorder, shape)
                    .clickable(enabled = enabled, role = Role.DropdownList) { expanded = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selected,
                    style = XpTheme.typography.fieldText,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    tint = colors.textLabel,
                    modifier = Modifier.size(16.dp),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = colors.surface,
            ) {
                options.forEach { category ->
                    val isSelected = category == selected
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = category,
                                style = XpTheme.typography.fieldText.copy(
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
                HorizontalDivider(color = colors.divider)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.new_category),
                            style = XpTheme.typography.fieldText.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_plus),
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(15.dp),
                        )
                    },
                    onClick = {
                        expanded = false
                        onNewCategory()
                    },
                )
            }
        }
    }
}

@Composable
private fun NewCategoryDialog(state: PasswordFormUiState, onAction: (PasswordFormAction) -> Unit) {
    val colors = XpTheme.colors
    AlertDialog(
        onDismissRequest = { if (!state.isCreatingCategory) onAction(PasswordFormAction.NewCategoryDismissed) },
        containerColor = colors.surface,
        title = {
            Text(
                text = stringResource(R.string.new_category_title),
                style = XpTheme.typography.dialogTitle,
                color = colors.textPrimary,
            )
        },
        text = {
            XpTextField(
                value = state.newCategoryName,
                onValueChange = { onAction(PasswordFormAction.NewCategoryNameChanged(it)) },
                placeholder = stringResource(R.string.new_category_placeholder),
                error = state.newCategoryError?.asString(),
                enabled = !state.isCreatingCategory,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            CompactButton(
                text = stringResource(R.string.create),
                icon = R.drawable.ic_plus,
                onClick = { onAction(PasswordFormAction.NewCategoryConfirmed) },
                style = CompactButtonStyle.Primary,
                isLoading = state.isCreatingCategory,
            )
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(PasswordFormAction.NewCategoryDismissed) },
                enabled = !state.isCreatingCategory,
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = XpTheme.typography.buttonSecondary,
                    color = colors.textSecondary,
                )
            }
        },
    )
}

@Preview(name = "Add", showBackground = true, heightDp = 1300)
@Composable
private fun PasswordFormScreenPreview() {
    XproKeyTheme(darkTheme = true) {
        PasswordFormScreen(
            state = PasswordFormUiState(
                title = "Github",
                url = "https://github.com",
                username = "Vanshgoel2610",
                password = "cVkd,FO)w[n09vR8",
                isPasswordVisible = true,
                strength = PasswordStrength(4),
                isFavorite = true,
                categories = listOf("Personal", "Work", "Finance", "Shopping", "Travel", "Social", "Others"),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(name = "Edit", showBackground = true, heightDp = 1300)
@Composable
private fun PasswordFormScreenEditPreview() {
    XproKeyTheme(darkTheme = false) {
        PasswordFormScreen(
            state = PasswordFormUiState(
                isEditing = true,
                title = "AppLock",
                url = "https://applock.com",
                username = "Vansh",
                category = "Travel",
                password = "wall",
                strength = PasswordStrength(0),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onBack = {},
        )
    }
}
