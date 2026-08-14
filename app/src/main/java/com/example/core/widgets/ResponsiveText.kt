package com.example.core.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.FrictionPrimary
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun ResponsiveText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    style: TextStyle = LocalTextStyle.current,
    enableTooltipOnTruncation: Boolean = true
) {
    // Determine if text is a short single-line phrase (e.g. Level 1, Daily Screen Time, Badges)
    val effectiveMaxLines = if (maxLines == Int.MAX_VALUE && !text.contains('\n') && text.length <= 40) {
        1
    } else {
        maxLines
    }

    val baseStyle = style.copy(
        fontSize = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize,
        lineBreak = LineBreak.Heading
    )

    var scaledTextStyle by remember(text, style, fontSize) {
        mutableStateOf(baseStyle)
    }
    var readyToDraw by remember(text) { mutableStateOf(false) }
    var isTruncated by remember(text) { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = if (enableTooltipOnTruncation && isTruncated) {
            modifier.clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                showTooltip = true
            }
        } else {
            modifier
        },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            modifier = Modifier.drawWithContent {
                if (readyToDraw) {
                    drawContent()
                }
            },
            color = color,
            fontSize = scaledTextStyle.fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily ?: scaledTextStyle.fontFamily ?: com.example.ui.theme.OutfitFontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = effectiveMaxLines,
            minLines = minLines,
            onTextLayout = { textLayoutResult ->
                val isExceedingLines = textLayoutResult.lineCount > effectiveMaxLines
                val hasEllipsis = textLayoutResult.isLineEllipsized(textLayoutResult.lineCount - 1)
                if (textLayoutResult.hasVisualOverflow || isExceedingLines || hasEllipsis) {
                    if (scaledTextStyle.fontSize.value > 9f) {
                        scaledTextStyle = scaledTextStyle.copy(
                            fontSize = scaledTextStyle.fontSize * 0.88f
                        )
                    } else {
                        isTruncated = true
                        readyToDraw = true
                    }
                } else {
                    readyToDraw = true
                }
            },
            style = scaledTextStyle
        )

        if (showTooltip && enableTooltipOnTruncation) {
            LaunchedEffect(showTooltip) {
                delay(3000L)
                showTooltip = false
            }

            Popup(
                alignment = Alignment.TopCenter,
                onDismissRequest = { showTooltip = false },
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, FrictionPrimary.copy(alpha = 0.5f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { showTooltip = false }
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
