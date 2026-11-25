package com.adveno.tv.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adveno.tv.R
import com.adveno.tv.presentation.theme.AdvenoButtonFocused
import com.adveno.tv.presentation.theme.AdvenoButtonUnfocused
import com.adveno.tv.presentation.theme.AdvenoError
import com.adveno.tv.presentation.theme.AdvenoFocusBorder
import com.adveno.tv.presentation.theme.AdvenoPrimary
import com.adveno.tv.presentation.theme.AdvenoSecondary
import com.adveno.tv.presentation.theme.AdvenoSurface
import com.adveno.tv.presentation.theme.AdvenoTextPrimary
import com.adveno.tv.presentation.theme.AdvenoTextSecondary
import kotlinx.coroutines.delay

/**
 * Ultra-compact dedicated Admin PIN entry dialog for Android TV.
 *
 * Optimized to fit entirely within any TV viewport without vertical clipping:
 * - Total height ~270dp (fits easily in 360dp-540dp TV viewports).
 * - Fixed 20dp status/error slot prevents dialog height shifts and avoids pushing bottom buttons off-screen.
 * - Both "Cancel" and "Submit" buttons are permanently visible with crisp, high-contrast text.
 * - Initial focus lands on digit '1' to prevent accidental dismissal on key release.
 * - Dismissable ONLY via explicit Cancel button click or remote Back button.
 */
@Composable
fun AdminPinDialog(
    isVisible: Boolean,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onSubmitPin: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    var pin by remember { mutableStateOf("") }
    val initialFocusRequester = remember { FocusRequester() }

    // Reset entered PIN when dialog closes; focus on digit '1' when dialog opens
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            pin = ""
        } else {
            delay(150L)
            try {
                initialFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Remote back button cancels dialog safely
    BackHandler(enabled = isVisible && !isLoading) {
        pin = ""
        onCancel()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE60A0E17)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.94f),
            exit = fadeOut() + scaleOut(targetScale = 0.94f)
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 330.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AdvenoSurface)
                    .border(1.dp, Color(0x333478F6), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Header: Compact horizontal Row (Icon + Title + Subtitle)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color(0x1F3478F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_link),
                                contentDescription = "Admin Security",
                                modifier = Modifier.size(14.dp),
                                colorFilter = ColorFilter.tint(AdvenoPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = "Admin Access",
                                style = MaterialTheme.typography.titleMedium,
                                color = AdvenoTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Enter 6-digit administrator PIN",
                                style = MaterialTheme.typography.bodySmall,
                                color = AdvenoTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 6 Masked PIN Indicator Slots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 6) {
                            val isFilled = i < pin.length
                            val isCurrent = i == pin.length

                            val boxBorderColor = when {
                                errorMessage != null -> AdvenoError
                                isCurrent -> AdvenoSecondary
                                isFilled -> AdvenoPrimary
                                else -> Color(0x33FFFFFF)
                            }

                            Box(
                                modifier = Modifier
                                    .size(width = 30.dp, height = 30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isFilled) Color(0x263478F6) else Color(0x0FFFFFFF))
                                    .border(
                                        width = if (isCurrent) 2.dp else 1.dp,
                                        color = boxBorderColor,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFilled) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AdvenoSecondary)
                                    )
                                } else if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 10.dp, height = 2.dp)
                                            .background(Color(0x80FFFFFF))
                                    )
                                }
                            }
                        }
                    }

                    // Fixed-height Status/Error slot: avoids height jumping and never pushes buttons off-screen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (errorMessage != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x1FEF4444))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_warning),
                                    contentDescription = null,
                                    tint = AdvenoError,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = errorMessage,
                                    color = AdvenoError,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // TV Numeric Keypad (0-9, Clear, ⌫)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Row 1: 1, 2, 3
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TvKeypadButton(
                                text = "1",
                                modifier = Modifier.focusRequester(initialFocusRequester),
                                enabled = !isLoading && pin.length < 6
                            ) { pin += "1" }
                            TvKeypadButton(text = "2", enabled = !isLoading && pin.length < 6) { pin += "2" }
                            TvKeypadButton(text = "3", enabled = !isLoading && pin.length < 6) { pin += "3" }
                        }

                        // Row 2: 4, 5, 6
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TvKeypadButton(text = "4", enabled = !isLoading && pin.length < 6) { pin += "4" }
                            TvKeypadButton(text = "5", enabled = !isLoading && pin.length < 6) { pin += "5" }
                            TvKeypadButton(text = "6", enabled = !isLoading && pin.length < 6) { pin += "6" }
                        }

                        // Row 3: 7, 8, 9
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TvKeypadButton(text = "7", enabled = !isLoading && pin.length < 6) { pin += "7" }
                            TvKeypadButton(text = "8", enabled = !isLoading && pin.length < 6) { pin += "8" }
                            TvKeypadButton(text = "9", enabled = !isLoading && pin.length < 6) { pin += "9" }
                        }

                        // Row 4: Clear, 0, Backspace
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TvKeypadButton(
                                text = "Clear",
                                enabled = !isLoading && pin.isNotEmpty(),
                                isAction = true
                            ) {
                                pin = ""
                            }

                            TvKeypadButton(text = "0", enabled = !isLoading && pin.length < 6) { pin += "0" }

                            TvKeypadButton(
                                text = "⌫",
                                enabled = !isLoading && pin.isNotEmpty(),
                                isAction = true
                            ) {
                                if (pin.isNotEmpty()) {
                                    pin = pin.dropLast(1)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Buttons: Cancel and Submit (Side-by-side, guaranteed visible text and styling)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AdminDialogButton(
                            text = "Cancel",
                            onClick = {
                                pin = ""
                                onCancel()
                            },
                            isPrimary = false,
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f)
                        )

                        AdminDialogButton(
                            text = if (isLoading) "Verifying..." else "Submit",
                            onClick = {
                                if (pin.length == 6 && !isLoading) {
                                    val submittedPin = pin
                                    pin = ""
                                    onSubmitPin(submittedPin)
                                }
                            },
                            isPrimary = true,
                            enabled = pin.length == 6 && !isLoading,
                            isLoading = isLoading,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dedicated high-contrast action button for Admin PIN dialog.
 * Guarantees visible text, D-pad focus ring, and clean sizing without clipping.
 */
@Composable
private fun AdminDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "AdminDialogBtnScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPrimary && !enabled -> Color(0x331E3A8A) // Subdued blue container for disabled Submit
            !isPrimary && !enabled -> Color(0x1FFFFFFF)
            isFocused && isPrimary -> Color(0xFF1D4ED8)
            isFocused && !isPrimary -> Color(0xFF334155)
            isPrimary -> Color(0xFF2563EB)
            else -> Color(0x26475569) // Neutral slate gray for Cancel
        },
        animationSpec = tween(durationMillis = 150),
        label = "AdminDialogBtnBg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color(0xFF22D3EE)
            isPrimary && !enabled -> Color(0x333B82F6)
            isPrimary -> Color(0xFF60A5FA)
            else -> Color(0x4094A3B8)
        },
        animationSpec = tween(durationMillis = 150),
        label = "AdminDialogBtnBorder"
    )

    val textColor = when {
        isPrimary && !enabled -> Color(0x9993C5FD) // Clearly visible muted blue for disabled Submit
        !isPrimary && !enabled -> Color(0x66FFFFFF)
        else -> Color.White // Crisp white text
    }

    Box(
        modifier = modifier
            .height(32.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick
            )
            .focusable(enabled = enabled && !isLoading, interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = text,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Android TV D-pad focusable numeric keypad button.
 */
@Composable
private fun TvKeypadButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isAction: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "KeypadButtonScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color(0x0AFFFFFF)
            isFocused -> AdvenoButtonFocused
            isAction -> Color(0x1FFFFFFF)
            else -> AdvenoButtonUnfocused
        },
        animationSpec = tween(durationMillis = 150),
        label = "KeypadButtonBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) AdvenoFocusBorder else Color(0x1AFFFFFF),
        animationSpec = tween(durationMillis = 150),
        label = "KeypadButtonBorder"
    )

    Box(
        modifier = modifier
            .size(width = 56.dp, height = 28.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .focusable(enabled = enabled, interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) AdvenoTextPrimary else Color(0x4DFFFFFF),
            fontSize = if (isAction) 11.sp else 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
