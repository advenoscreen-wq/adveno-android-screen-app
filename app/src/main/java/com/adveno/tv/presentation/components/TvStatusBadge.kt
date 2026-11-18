package com.adveno.tv.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adveno.tv.presentation.theme.AdvenoDimensions
import com.adveno.tv.presentation.theme.AdvenoShapes
import com.adveno.tv.presentation.theme.AdvenoSurface
import com.adveno.tv.presentation.theme.AdvenoTextPrimary

/**
 * Reusable Status Badge for digital signage displays.
 * Displays a glowing colored indicator dot alongside clear status text.
 */
@Composable
fun TvStatusBadge(
    statusText: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(AdvenoShapes.small)
            .background(AdvenoSurface.copy(alpha = 0.85f))
            .padding(
                horizontal = AdvenoDimensions.spaceMd,
                vertical = AdvenoDimensions.spaceSm
            )
    ) {
        // Glowing status dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(statusColor)
        )

        Spacer(modifier = Modifier.width(AdvenoDimensions.spaceSm))

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = AdvenoTextPrimary
        )
    }
}

