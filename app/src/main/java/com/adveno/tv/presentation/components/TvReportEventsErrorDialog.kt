package com.adveno.tv.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adveno.tv.R
import com.adveno.tv.presentation.theme.AdvenoError
import com.adveno.tv.presentation.theme.AdvenoPrimary
import com.adveno.tv.presentation.theme.AdvenoSurface
import com.adveno.tv.presentation.theme.AdvenoTextPrimary
import com.adveno.tv.presentation.theme.AdvenoTextSecondary

/**
 * Android TV Modal Dialog displayed when playback event reporting fails.
 *
 * Displays:
 * - Scrim background dimming the current screen.
 * - Warning icon.
 * - "Report Failed" heading.
 * - Server / Network error message.
 * - Side-by-side 'Cancel' and 'Retry' buttons designed for Android TV D-pad navigation.
 * - Remains visible until explicit user interaction.
 */
@Composable
fun TvReportEventsErrorDialog(
    isVisible: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    BackHandler(enabled = isVisible) {
        onCancel()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xD90D131F)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + scaleOut(targetScale = 0.92f)
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 360.dp, max = 500.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AdvenoSurface)
                    .border(1.dp, Color(0x33EF4444), RoundedCornerShape(20.dp))
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0x1FEF4444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_warning),
                            contentDescription = "Report Error",
                            modifier = Modifier.size(28.dp),
                            colorFilter = ColorFilter.tint(AdvenoError)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Report Failed",
                        style = MaterialTheme.typography.titleLarge,
                        color = AdvenoTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = errorMessage ?: "Unable to report pending playback events to the server. Please check your network and retry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AdvenoTextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvButton(
                            text = "Cancel",
                            onClick = onCancel,
                            modifier = Modifier.widthIn(min = 130.dp)
                        )

                        TvButton(
                            text = "Retry",
                            onClick = onRetry,
                            modifier = Modifier.widthIn(min = 130.dp)
                        )
                    }
                }
            }
        }
    }
}
