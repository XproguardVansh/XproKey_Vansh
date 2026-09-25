package com.xprokeey2.presentation.legal

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprokeey2.R
import com.xprokeey2.presentation.theme.Manrope
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme

/** Blue -> cyan used on the highlighted word of the title ("Service", "Policy"). */
private val TitleGradient = Brush.horizontalGradient(listOf(Color(0xFF0767FB), Color(0xFF00CFF3)))

private val CardShape = RoundedCornerShape(20.dp)
private val TileShape = RoundedCornerShape(12.dp)
private val PillShape = RoundedCornerShape(12.dp)

@Composable
fun LegalDocumentScreen(
    document: LegalDocument,
    onBack: () -> Unit,
) {
    val palette = legalPalette()

    Scaffold(containerColor = palette.page) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BackButton(
                    onClick = onBack,
                    palette = palette,
                    modifier = Modifier.align(Alignment.Start),
                )

                Spacer(Modifier.height(32.dp))
                Header(document = document, palette = palette)

                Spacer(Modifier.height(32.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    document.sections.forEach { section ->
                        SectionCard(section = section, palette = palette)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit, palette: LegalPalette, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(palette.button)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_left),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.legal_back),
            style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 14.sp),
            color = Color.White,
        )
    }
}

@Composable
private fun Header(document: LegalDocument, palette: LegalPalette) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(palette.badgeBackground)
            .border(1.dp, palette.badgeBorder, RoundedCornerShape(99.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(document.badgeIcon),
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(document.badge),
            style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
            color = palette.accent,
        )
    }

    Spacer(Modifier.height(20.dp))
    Text(
        text = buildAnnotatedString {
            append(stringResource(document.titlePrefix))
            append(" ")
            withStyle(SpanStyle(brush = TitleGradient)) {
                append(stringResource(document.titleHighlight))
            }
        },
        style = TextStyle(
            fontFamily = Manrope,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 36.sp,
            lineHeight = 42.sp,
            letterSpacing = (-0.8).sp,
        ),
        color = palette.title,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
    )

    Spacer(Modifier.height(14.dp))
    Text(
        text = stringResource(document.subtitle),
        style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 22.sp),
        color = palette.muted,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.legal_last_updated),
        style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 12.sp),
        color = palette.lastUpdated,
    )
}

@Composable
private fun SectionCard(section: LegalSection, palette: LegalPalette) {
    val background = if (section.highlighted) palette.highlightBackground else palette.card
    val border = if (section.highlighted) palette.highlightBorder else palette.cardBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(background)
            .border(1.dp, border, CardShape)
            .padding(20.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(TileShape)
                .background(if (section.highlighted) palette.highlightTile else palette.iconTile),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(section.icon),
                contentDescription = null,
                tint = if (section.highlighted) Color.White else palette.accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(section.title),
                style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, lineHeight = 24.sp),
                color = palette.title,
                modifier = Modifier.semantics { heading() },
            )
            section.subtitle?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(it),
                    style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
                    color = palette.muted,
                )
            }
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                section.blocks.forEach { block -> LegalBlockContent(block = block, palette = palette) }
            }
        }
    }
}

private val BodyStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 22.sp)

@Composable
private fun LegalBlockContent(block: LegalBlock, palette: LegalPalette) {
    when (block) {
        is LegalBlock.Paragraph -> Text(
            text = stringResource(block.text),
            style = BodyStyle,
            color = palette.body,
        )

        is LegalBlock.RichParagraph -> Text(
            text = buildAnnotatedString {
                append(stringResource(block.prefix))
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(stringResource(block.bold)) }
                append(" ")
                append(stringResource(block.suffix))
            },
            style = BodyStyle.copy(fontWeight = FontWeight.SemiBold),
            color = palette.title,
        )

        is LegalBlock.Bullets -> BulletList(items = block.items, palette = palette)

        is LegalBlock.SubHeading -> Text(
            text = stringResource(block.text),
            style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
            color = palette.title,
            modifier = Modifier.padding(top = 4.dp),
        )

        is LegalBlock.Note -> Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(stringResource(block.label)) }
                append(" ")
                append(stringResource(block.text))
            },
            style = BodyStyle.copy(fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
            color = palette.accent,
            modifier = Modifier
                .fillMaxWidth()
                .clip(TileShape)
                .background(palette.noteBackground)
                .border(1.dp, palette.noteBorder, TileShape)
                .padding(14.dp),
        )

        is LegalBlock.LabeledList -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.listBackground)
                .border(1.dp, palette.listBorder, RoundedCornerShape(14.dp))
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(block.heading).uppercase(),
                style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 0.8.sp),
                color = palette.listHeading,
            )
            Spacer(Modifier.height(8.dp))
            BulletList(items = block.items, palette = palette, fontSize = 13)
        }

        LegalBlock.ContactEmail -> ContactChip(palette = palette)
    }
}

@Composable
private fun BulletList(items: List<Int>, palette: LegalPalette, fontSize: Int = 14) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            // Hanging indent: wrapped lines line up with the text, not the bullet.
            Row {
                Text(text = "•", style = BodyStyle.copy(fontSize = fontSize.sp), color = palette.body)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(item),
                    style = BodyStyle.copy(fontSize = fontSize.sp),
                    color = palette.body,
                )
            }
        }
    }
}

@Composable
private fun ContactChip(palette: LegalPalette) {
    val context = LocalContext.current
    val email = stringResource(R.string.legal_contact_email)
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(palette.button)
            .clickable(role = Role.Button) {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                try {
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    // No email app installed; nothing sensible to open.
                }
            }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_mail),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = email,
            style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = Color.White,
        )
    }
}

/** Dark values sampled from the web pages; light values follow the app's light theme. */
@Immutable
private data class LegalPalette(
    val page: Color,
    val card: Color,
    val cardBorder: Color,
    val iconTile: Color,
    val accent: Color,
    val button: Color,
    val badgeBackground: Color,
    val badgeBorder: Color,
    val title: Color,
    val body: Color,
    val muted: Color,
    val lastUpdated: Color,
    val highlightBackground: Color,
    val highlightBorder: Color,
    val highlightTile: Color,
    val noteBackground: Color,
    val noteBorder: Color,
    val listBackground: Color,
    val listBorder: Color,
    val listHeading: Color,
)

@Composable
private fun legalPalette(): LegalPalette {
    val colors = XpTheme.colors
    return if (colors.isDark) {
        LegalPalette(
            page = Color(0xFF0D1117),
            card = Color(0xFF020618),
            cardBorder = Color(0xFF0F172B),
            iconTile = Color(0xFF0A1330),
            accent = Color(0xFF50A2FF),
            button = Color(0xFF3471E4),
            badgeBackground = Color(0xFF111930),
            badgeBorder = Color(0xFF152655),
            title = Color.White,
            body = Color(0xFF90A1B9),
            muted = Color(0xFF64748B),
            lastUpdated = Color(0xFFCBD5E1),
            highlightBackground = Color(0xFF0C1B27),
            highlightBorder = Color(0xFF50A2FF).copy(alpha = 0.30f),
            highlightTile = Color(0xFF2B7FFF),
            noteBackground = Color(0xFF040C24),
            noteBorder = Color(0xFF50A2FF).copy(alpha = 0.22f),
            listBackground = Color(0xFF070D1F),
            listBorder = Color(0xFF757A87),
            listHeading = Color(0xFFCAD5E2),
        )
    } else {
        LegalPalette(
            page = colors.background,
            card = colors.surface,
            cardBorder = colors.fieldBorder,
            iconTile = colors.primary.copy(alpha = 0.10f),
            accent = colors.primary,
            button = colors.primary,
            badgeBackground = colors.primary.copy(alpha = 0.08f),
            badgeBorder = colors.primary.copy(alpha = 0.25f),
            title = colors.textPrimary,
            body = colors.textSecondary,
            muted = colors.textLabel,
            lastUpdated = colors.textPrimary,
            highlightBackground = Color(0xFFF1F5FF),
            highlightBorder = colors.primary.copy(alpha = 0.30f),
            highlightTile = colors.primary,
            noteBackground = colors.primary.copy(alpha = 0.06f),
            noteBorder = colors.primary.copy(alpha = 0.20f),
            listBackground = colors.fieldBackground,
            listBorder = colors.fieldBorder,
            listHeading = colors.textPrimary,
        )
    }
}

@Preview(name = "Terms - dark", heightDp = 1400)
@Composable
private fun TermsPreview() {
    XproKeyTheme(darkTheme = true) { LegalDocumentScreen(document = TermsOfService, onBack = {}) }
}

@Preview(name = "Privacy - light", heightDp = 1400)
@Composable
private fun PrivacyPreview() {
    XproKeyTheme(darkTheme = false) { LegalDocumentScreen(document = PrivacyPolicy, onBack = {}) }
}
