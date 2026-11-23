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
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.adveno.tv.presentation.theme.AdvenoSecondary
import com.adveno.tv.presentation.theme.AdvenoSuccess
import com.adveno.tv.presentation.theme.AdvenoSurface
import com.adveno.tv.presentation.theme.AdvenoTextPrimary
import com.adveno.tv.presentation.theme.AdvenoTextSecondary

/**
 * Android TV D-pad friendly modal dialog presented when internet connectivity is interrupted.
 * Informs the user that progress is saved, displays the interrupted operation,
 * and allows resuming once connectivity is restored.
 */
@Composable
fun TvNoInternetModal(
    isVisible: Boolean,
    interruptedOperation: String,
    statusMessage: String? = null,
    isConnectionRestored: Boolean = false,
    onRetryConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    // Prevent remote back button from leaving in inconsistent state while modal is active
    BackHandler(enabled = isVisible) {
        // Back press also triggers retry check
        onRetryConnection()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xD90D131F)), // Dark slate scrim
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + scaleOut(targetScale = 0.92f)
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 360.dp, max = 560.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AdvenoSurface)
                    .border(
                        width = 1.5.dp,
                        color = if (isConnectionRestored) AdvenoSuccess else Color(0x33EF4444),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Wi-Fi Disconnected / Restored Icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(
                                if (isConnectionRestored) Color(0x1F22C55E) else Color(0x1FEF4444)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(
                                id = if (isConnectionRestored) R.drawable.ic_check else R.drawable.ic_wifi_off
                            ),
                            contentDescription = "Network Status Icon",
                            colorFilter = ColorFilter.tint(
                                if (isConnectionRestored) AdvenoSuccess else AdvenoError
                            ),
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Heading
                    Text(
                        text = if (isConnectionRestored) "Connection Restored" else "No Internet Connection",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AdvenoTextPrimary,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Explanation that progress is saved
                    Text(
                        text = "Please check your internet connection. Your progress is saved.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AdvenoTextSecondary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Interrupted Operation Indicator Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF101522))
                            .border(1.dp, Color(0xFF283548), RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Interrupted at: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = AdvenoTextSecondary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = interruptedOperation.ifBlank { "Operation in progress" },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = AdvenoSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Dynamic feedback when user clicked Retry but internet was still unavailable
                    if (!statusMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isConnectionRestored) AdvenoSuccess else AdvenoError,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // TV Action Button
                    TvButton(
                        text = "Retry Connection",
                        onClick = onRetryConnection,
                        modifier = Modifier.fillMaxWidth(0.75f)
                    )
                }
            }
        }
    }
}
