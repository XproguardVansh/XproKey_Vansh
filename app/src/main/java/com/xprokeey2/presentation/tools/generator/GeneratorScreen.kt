package com.xprokeey2.presentation.tools.generator

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.usecase.vault.GeneratedPasswordStrength
import com.xprokeey2.domain.usecase.vault.PasswordGeneratorOptions
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.copyToClipboard
import com.xprokeey2.presentation.workspace.PageBadge
import com.xprokeey2.presentation.workspace.UserBadge
import com.xprokeey2.presentation.workspace.WorkspacePanel
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val LengthMarks = listOf(4, 12, 24, 40)
private val ThumbSize = 20.dp

@Composable
fun GeneratorScreenRoot(
    onSectionClick: (WorkspaceSection) -> Unit,
    viewModel: GeneratorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // The copy button shows a tick for 1.2 s, like the web.
    var copiedPassword by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(copiedPassword) {
        if (copiedPassword != null) {
            delay(1200)
            copiedPassword = null
        }
    }

    GeneratorScreen(
        state = state,
        isCopied = copiedPassword != null && copiedPassword == state.password,
        snackbarHostState = snackbarHostState,
        onSectionClick = onSectionClick,
        onAction = viewModel::onAction,
        onCopy = {
            val password = state.password
            if (password.isNotEmpty()) {
                copyToClipboard(context, label = context.getString(R.string.label_password), value = password, sensitive = true)
                copiedPassword = password
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.generator_copied)) }
            }
        },
    )
}

@Composable
fun GeneratorScreen(
    state: GeneratorUiState,
    isCopied: Boolean,
    snackbarHostState: SnackbarHostState,
    onSectionClick: (WorkspaceSection) -> Unit,
    onAction: (GeneratorAction) -> Unit,
    onCopy: () -> Unit,
) {
    val colors = XpTheme.colors
    WorkspaceScaffold(
        user = state.user,
        currentSection = WorkspaceSection.GENERATOR,
        onSectionClick = onSectionClick,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.generator_title),
                    style = XpTheme.typography.pageTitle,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                PageBadge(stringResource(R.string.generator_badge))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.generator_subtitle),
                style = XpTheme.typography.body.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(24.dp))
            OutputCard(
                state = state,
                isCopied = isCopied,
                onRegenerate = { onAction(GeneratorAction.Regenerate) },
                onCopy = onCopy,
            )

            Spacer(Modifier.height(20.dp))
            OptionsCard(
                options = state.options,
                onOptionsChange = { onAction(GeneratorAction.OptionsChanged(it)) },
            )
        }
    }
}

@Composable
private fun OutputCard(
    state: GeneratorUiState,
    isCopied: Boolean,
    onRegenerate: () -> Unit,
    onCopy: () -> Unit,
) {
    val colors = XpTheme.colors
    val hasAnyCharset = state.options.hasAnyCharset
    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.generator_output_label).uppercase(),
                style = XpTheme.typography.fieldLabel,
                color = colors.textLabel,
                modifier = Modifier.weight(1f),
            )
            if (hasAnyCharset && state.password.isNotEmpty()) StrengthPill(state.strength)
        }

        Spacer(Modifier.height(16.dp))
        val boxShape = RoundedCornerShape(12.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(boxShape)
                .background(colors.fieldBackground)
                .border(1.dp, colors.fieldBorder, boxShape)
                .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (hasAnyCharset) state.password else stringResource(R.string.generator_select_option),
                style = if (hasAnyCharset) {
                    XpTheme.typography.mono.copy(fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Bold)
                } else {
                    XpTheme.typography.body.copy(fontSize = 14.sp)
                },
                color = if (hasAnyCharset) colors.textPrimary else colors.textLabel,
                modifier = Modifier.weight(1f),
            )
            BoxIconButton(
                icon = R.drawable.ic_refresh,
                description = stringResource(R.string.cd_regenerate),
                tint = colors.textSecondary,
                enabled = hasAnyCharset,
                onClick = onRegenerate,
            )
            BoxIconButton(
                icon = if (isCopied) R.drawable.ic_check else R.drawable.ic_copy,
                description = stringResource(R.string.cd_copy_password),
                tint = if (isCopied) EmeraldLight else colors.textSecondary,
                enabled = hasAnyCharset && state.password.isNotEmpty(),
                onClick = onCopy,
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.generator_strength_label),
                style = XpTheme.typography.bodyBold.copy(fontSize = 12.5.sp),
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (hasAnyCharset) stringResource(R.string.generator_bits, state.entropyBits) else stringResource(R.string.stat_unknown),
                style = XpTheme.typography.bodyBold.copy(fontSize = 12.5.sp),
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.height(10.dp))
        StrengthBar(fraction = if (hasAnyCharset) state.strength.percentage / 100f else 0f)

        Spacer(Modifier.height(14.dp))
        val bannerShape = RoundedCornerShape(12.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(bannerShape)
                .background(if (colors.isDark) colors.surface else colors.primary.copy(alpha = 0.04f))
                .border(1.dp, if (colors.isDark) colors.divider else colors.primary.copy(alpha = 0.14f), bannerShape)
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shield),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(15.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.generator_info),
                style = XpTheme.typography.body.copy(fontSize = 11.5.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
                color = colors.textSecondary,
            )
        }

        if (!hasAnyCharset) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.generator_no_charset),
                style = XpTheme.typography.body.copy(fontSize = 12.sp),
                color = colors.error,
            )
        }
    }
}

/** The web's Weak / Fair / Good / Strong / Excellent pill. */
@Composable
private fun StrengthPill(strength: GeneratedPasswordStrength) {
    val isDark = XpTheme.colors.isDark
    val (label, tone) = when (strength) {
        GeneratedPasswordStrength.WEAK -> R.string.generator_weak to PillTone.Red
        GeneratedPasswordStrength.FAIR -> R.string.generator_fair to PillTone.Orange
        GeneratedPasswordStrength.GOOD -> R.string.generator_good to PillTone.Yellow
        GeneratedPasswordStrength.STRONG -> R.string.generator_strong to PillTone.Emerald
        GeneratedPasswordStrength.EXCELLENT -> R.string.generator_excellent to PillTone.Emerald
    }
    val shape = RoundedCornerShape(50)
    Text(
        text = stringResource(label).uppercase(),
        style = XpTheme.typography.caption.copy(fontSize = 10.sp, letterSpacing = 0.8.sp),
        color = if (isDark) tone.darkText else tone.text,
        modifier = Modifier
            .clip(shape)
            .background(tone.base.copy(alpha = if (isDark) 0.08f else 0.1f))
            .border(1.dp, if (isDark) tone.base.copy(alpha = 0.2f) else tone.lightBorder, shape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Tailwind colours of the web pill: -500 base, -600 text (-400 in dark), -100 border. */
private enum class PillTone(val base: Color, val text: Color, val darkText: Color, val lightBorder: Color) {
    Red(Color(0xFFEF4444), Color(0xFFDC2626), Color(0xFFF87171), Color(0xFFFEE2E2)),
    Orange(Color(0xFFF97316), Color(0xFFEA580C), Color(0xFFFB923C), Color(0xFFFFEDD5)),
    Yellow(Color(0xFFEAB308), Color(0xFFCA8A04), Color(0xFFFACC15), Color(0xFFFEF9C3)),
    Emerald(Color(0xFF10B981), Color(0xFF059669), Color(0xFF34D399), Color(0xFFD1FAE5)),
}

private val EmeraldLight = Color(0xFF10B981)

/** Red → yellow → green bar filled to the rating (20 % … 100 %). */
@Composable
private fun StrengthBar(fraction: Float) {
    val animated by animateFloatAsState(targetValue = fraction, label = "strengthBar")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(XpTheme.colors.divider),
    ) {
        if (animated > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFEAB308), Color(0xFF10B981)))),
            )
        }
    }
}

@Composable
private fun OptionsCard(options: PasswordGeneratorOptions, onOptionsChange: (PasswordGeneratorOptions) -> Unit) {
    val colors = XpTheme.colors
    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_settings_2),
                contentDescription = null,
                tint = colors.textLabel,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.generator_options).uppercase(),
                style = XpTheme.typography.bodyBold.copy(fontSize = 13.sp, letterSpacing = 1.sp),
                color = colors.textPrimary,
            )
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = colors.divider)

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.generator_length),
                style = XpTheme.typography.bodyBold.copy(fontSize = 12.5.sp),
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.fieldBackground)
                    .border(1.dp, colors.fieldBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = options.length.toString(), style = XpTheme.typography.bodyBold, color = colors.textPrimary)
            }
        }
        Spacer(Modifier.height(4.dp))
        LengthSlider(length = options.length, onLengthChange = { onOptionsChange(options.copy(length = it)) })
        LengthMarksRow()

        Spacer(Modifier.height(18.dp))
        OptionRow(
            label = stringResource(R.string.generator_uppercase),
            description = stringResource(R.string.generator_uppercase_desc),
            checked = options.uppercase,
            onCheckedChange = { onOptionsChange(options.copy(uppercase = it)) },
        )
        OptionRow(
            label = stringResource(R.string.generator_lowercase),
            description = stringResource(R.string.generator_lowercase_desc),
            checked = options.lowercase,
            onCheckedChange = { onOptionsChange(options.copy(lowercase = it)) },
        )
        OptionRow(
            label = stringResource(R.string.generator_numbers),
            description = stringResource(R.string.generator_numbers_desc),
            checked = options.numbers,
            onCheckedChange = { onOptionsChange(options.copy(numbers = it)) },
        )
        OptionRow(
            label = stringResource(R.string.generator_symbols),
            description = stringResource(R.string.generator_symbols_desc),
            checked = options.symbols,
            onCheckedChange = { onOptionsChange(options.copy(symbols = it)) },
        )

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = colors.divider)
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.generator_advanced).uppercase(),
            style = XpTheme.typography.fieldLabel.copy(letterSpacing = 1.4.sp),
            color = colors.textLabel,
        )
        Spacer(Modifier.height(8.dp))
        OptionRow(
            label = stringResource(R.string.generator_avoid_ambiguous),
            description = stringResource(R.string.generator_avoid_ambiguous_desc),
            checked = options.avoidAmbiguous,
            onCheckedChange = { onOptionsChange(options.copy(avoidAmbiguous = it)) },
        )
        OptionRow(
            label = stringResource(R.string.generator_require_all),
            description = stringResource(R.string.generator_require_all_desc),
            checked = options.requireAllTypes,
            onCheckedChange = { onOptionsChange(options.copy(requireAllTypes = it)) },
        )
    }
}

/** Length 4–40 in steps of 1: dark track, white round thumb (the web's slider). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LengthSlider(length: Int, onLengthChange: (Int) -> Unit) {
    val colors = XpTheme.colors
    Slider(
        value = length.toFloat(),
        onValueChange = { value ->
            val newLength = value.roundToInt()
            if (newLength != length) onLengthChange(newLength)
        },
        valueRange = MIN_LENGTH.toFloat()..MAX_LENGTH.toFloat(),
        steps = MAX_LENGTH - MIN_LENGTH - 1,
        thumb = {
            Box(
                modifier = Modifier
                    .size(ThumbSize)
                    .shadow(2.dp, CircleShape)
                    .background(Color.White, CircleShape)
                    .border(2.dp, colors.textPrimary, CircleShape),
            )
        },
        track = { sliderState ->
            val range = sliderState.valueRange
            val fraction = ((sliderState.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(colors.divider),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(colors.textPrimary),
                )
            }
        },
    )
}

/** 4, 12, 24 and 40 under the slider, each centred where the thumb would be for that length. */
@Composable
private fun LengthMarksRow() {
    val style = XpTheme.typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    val color = XpTheme.colors.textLabel
    Layout(
        content = { LengthMarks.forEach { Text(text = it.toString(), style = style, color = color) } },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints()) }
        val width = constraints.maxWidth
        val inset = (ThumbSize / 2).roundToPx()
        val usable = width - 2 * inset
        layout(width, placeables.maxOf { it.height }) {
            placeables.forEachIndexed { index, placeable ->
                val fraction = (LengthMarks[index] - MIN_LENGTH) / (MAX_LENGTH - MIN_LENGTH).toFloat()
                val center = inset + (usable * fraction).roundToInt()
                placeable.placeRelative(center - placeable.width / 2, 0)
            }
        }
    }
}

/** A setting with its description and a switch; the whole row toggles it. */
@Composable
private fun OptionRow(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = XpTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = XpTheme.typography.bodyBold.copy(fontSize = 13.sp), color = colors.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = XpTheme.typography.body.copy(fontSize = 11.sp, lineHeight = 14.sp),
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.width(12.dp))
        SwitchVisual(checked = checked)
    }
}

/** The web's small switch: blue when on, grey when off. Clicks are handled by the row. */
@Composable
private fun SwitchVisual(checked: Boolean) {
    val colors = XpTheme.colors
    val track by animateColorAsState(if (checked) colors.primary else colors.fieldBorder, label = "switchTrack")
    val thumbOffset by animateDpAsState(if (checked) 18.dp else 2.dp, label = "switchThumb")
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 20.dp)
            .clip(CircleShape)
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 2.dp)
                .size(16.dp)
                .shadow(1.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

@Composable
private fun BoxIconButton(
    @DrawableRes icon: Int,
    description: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun GeneratorScreenPreview() {
    XproKeyTheme(darkTheme = false) {
        GeneratorScreen(
            state = GeneratorUiState(
                user = UserBadge.from("Vansh Goel", "goelv2610@gmail.com"),
                password = "abCR32A?[R-1970vZ>U)",
            ),
            isCopied = false,
            snackbarHostState = remember { SnackbarHostState() },
            onSectionClick = {},
            onAction = {},
            onCopy = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun GeneratorScreenNoOptionsPreview() {
    XproKeyTheme(darkTheme = true) {
        GeneratorScreen(
            state = GeneratorUiState(
                user = UserBadge.from("Vansh Goel", "goelv2610@gmail.com"),
                options = PasswordGeneratorOptions(length = 20, uppercase = false, lowercase = false, numbers = false, symbols = false),
            ),
            isCopied = false,
            snackbarHostState = remember { SnackbarHostState() },
            onSectionClick = {},
            onAction = {},
            onCopy = {},
        )
    }
}
