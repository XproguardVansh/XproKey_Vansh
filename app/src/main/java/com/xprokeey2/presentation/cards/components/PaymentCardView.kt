package com.xprokeey2.presentation.cards.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprokeey2.R
import com.xprokeey2.domain.model.CardBrand
import com.xprokeey2.presentation.theme.Manrope
import com.xprokeey2.presentation.theme.SpaceGrotesk
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme

private val CardShape = RoundedCornerShape(16.dp)

/** Card colours from the Figma card designs; RuPay green is taken from the web app. */
private class CardFace(val gradient: List<Color>, val highlight: Float, val silverChip: Boolean = false)

private val BlueFace = CardFace(listOf(Color(0xFF0B2C74), Color(0xFF1A52AD), Color(0xFF0A3F93)), highlight = 0.26f)
private val GraphiteFace = CardFace(listOf(Color(0xFF16161B), Color(0xFF2C2C33), Color(0xFF3A3A42)), highlight = 0.16f)
private val TealFace = CardFace(listOf(Color(0xFF1796D4), Color(0xFF0F6FAE), Color(0xFF0A567F)), highlight = 0.28f, silverChip = true)
private val GreenFace = CardFace(listOf(Color(0xFF0E6B45), Color(0xFF128153), Color(0xFF17A56A)), highlight = 0.24f)

private val CardBrand.face: CardFace
    get() = when (this) {
        CardBrand.RUPAY -> GreenFace
        CardBrand.AMEX -> TealFace
        CardBrand.MASTERCARD, CardBrand.DINERS, CardBrand.DISCOVER, CardBrand.JCB, CardBrand.UNIONPAY -> GraphiteFace
        CardBrand.VISA, CardBrand.UNKNOWN -> BlueFace
    }

/**
 * The payment card drawn in the web app's style. [number] is shown as given (masked, revealed or
 * a live preview), so callers decide how much of it is visible.
 */
@Composable
fun PaymentCardView(
    label: String,
    brand: CardBrand,
    number: String,
    holderName: String,
    expiry: String,
    modifier: Modifier = Modifier,
) {
    val face = brand.face
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .shadow(elevation = 14.dp, shape = CardShape, spotColor = face.gradient[1])
            .clip(CardShape)
            .drawBehind {
                drawRect(
                    Brush.linearGradient(
                        0f to face.gradient[0],
                        0.55f to face.gradient[1],
                        1f to face.gradient[2],
                        start = Offset(size.width * 0.09f, -size.height * 0.15f),
                        end = Offset(size.width * 0.91f, size.height * 1.15f),
                    )
                )
                // Soft light in the top-left corner and a diagonal sheen, as in Figma.
                drawRect(
                    Brush.radialGradient(
                        0f to Color.White.copy(alpha = face.highlight),
                        0.46f to Color.Transparent,
                        center = Offset(size.width * 0.12f, size.height * 0.06f),
                        radius = size.width * 1.3f,
                    )
                )
                drawRect(
                    Brush.linearGradient(
                        0.30f to Color.Transparent,
                        0.40f to Color.White.copy(alpha = 0.12f),
                        0.50f to Color.Transparent,
                        start = Offset.Zero,
                        end = Offset(size.width, size.height * 0.8f),
                    )
                )
            }
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label.uppercase().ifBlank { " " },
                        style = XpTheme.typography.cardCaption.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.card_secure_caption),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
                Text(
                    text = stringResource(R.string.brand_name),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = (-0.2).sp,
                    color = Color.White,
                )
            }

            CardChip(silver = face.silverChip)

            Text(
                text = number,
                style = XpTheme.typography.cardNumber,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
            )

            Row(verticalAlignment = Alignment.Bottom) {
                CardCaption(
                    caption = stringResource(R.string.card_holder_caption),
                    value = holderName.uppercase().ifBlank { stringResource(R.string.card_holder_placeholder).uppercase() },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                CardCaption(
                    caption = stringResource(R.string.card_expires_caption),
                    value = expiry.ifBlank { stringResource(R.string.card_expiry_placeholder) },
                )
                Spacer(Modifier.width(16.dp))
                CardBrandMark(brand = brand)
            }
        }
    }
}

@Composable
private fun CardCaption(caption: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = caption.uppercase(),
            style = XpTheme.typography.cardCaption,
            color = Color.White.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            fontFamily = Manrope,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 0.6.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Gold EMV chip (silver on Amex), drawn rather than shipped as an image. */
@Composable
private fun CardChip(silver: Boolean) {
    val base = if (silver) {
        listOf(Color(0xFFEEF2F6), Color(0xFFC2CCD6), Color(0xFF9AA6B2))
    } else {
        listOf(Color(0xFFFBE79A), Color(0xFFE6C25C), Color(0xFFCAA23F))
    }
    val line = if (silver) Color(0x47283C50) else Color(0x5278500A)
    Canvas(modifier = Modifier.size(width = 44.dp, height = 33.dp)) {
        val radius = CornerRadius(6.dp.toPx())
        drawRoundRect(
            Brush.linearGradient(0f to base[0], 0.42f to base[1], 1f to base[2], end = Offset(size.width, size.height)),
            cornerRadius = radius,
        )
        val stroke = 1.dp.toPx()
        for (fraction in listOf(0.34f, 0.66f)) {
            drawLine(line, Offset(0f, size.height * fraction), Offset(size.width, size.height * fraction), stroke)
        }
        for (fraction in listOf(0.3f, 0.7f)) {
            drawLine(line, Offset(size.width * fraction, 0f), Offset(size.width * fraction, size.height), stroke)
        }
        drawRoundRect(
            color = base[1],
            topLeft = Offset(size.width * 0.34f, size.height * 0.2f),
            size = Size(size.width * 0.32f, size.height * 0.6f),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )
        drawRoundRect(
            color = line,
            topLeft = Offset(size.width * 0.34f, size.height * 0.2f),
            size = Size(size.width * 0.32f, size.height * 0.6f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(width = stroke),
        )
    }
}

/** Network wordmark in the bottom-right corner. */
@Composable
fun CardBrandMark(brand: CardBrand, modifier: Modifier = Modifier) {
    when (brand) {
        CardBrand.MASTERCARD -> Box(modifier = modifier.size(width = 38.dp, height = 24.dp)) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFEB001B))
            )
            Box(
                Modifier
                    .offset(x = 14.dp)
                    .size(24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFF79E1B).copy(alpha = 0.9f))
            )
        }
        CardBrand.AMEX -> Text(
            text = "AMEX",
            modifier = modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Color(0xFF0F6FAE),
        )
        CardBrand.RUPAY -> Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 18.dp, height = 9.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF4F46E5), Color(0xFF6366F1))))
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "RuPay",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White,
            )
        }
        CardBrand.UNKNOWN -> Unit
        else -> Text(
            text = if (brand == CardBrand.VISA) "VISA" else brand.displayName.uppercase(),
            modifier = modifier,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = if (brand == CardBrand.VISA) 20.sp else 13.sp,
            letterSpacing = (-0.5).sp,
            color = Color.White,
        )
    }
}

@Preview(widthDp = 380)
@Composable
private fun PaymentCardViewPreview() {
    XproKeyTheme(darkTheme = true) {
        Column(
            modifier = Modifier.background(XpTheme.colors.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PaymentCardView("Personal", CardBrand.RUPAY, maskedCardNumber("2086"), "Vansh Goel", "07/28")
            PaymentCardView("Business", CardBrand.MASTERCARD, "5454 5454 5454 4421", "Aarav M.", "11/27")
            PaymentCardView("Travel", CardBrand.AMEX, formatCardNumber("378282246310005"), "Aarav M.", "02/28")
            PaymentCardView("", CardBrand.VISA, previewCardNumber("4111"), "", "")
        }
    }
}
