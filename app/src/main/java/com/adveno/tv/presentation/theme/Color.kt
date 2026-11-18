package com.adveno.tv.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Adveno Premium Digital Signage Color Palette
 * Specifically optimized for high-contrast visibility on 720p, 1080p, and 4K TV displays.
 */

// Core Brand Colors
val AdvenoBackground = Color(0xFF101522)       // Deep slate navy canvas
val AdvenoPrimary = Color(0xFF3478F6)          // Electric Blue brand primary
val AdvenoPrimaryDark = Color(0xFF1D4ED8)      // Darker shade for active/pressed states
val AdvenoSecondary = Color(0xFF22D3EE)        // Cyan highlight & focus glow
val AdvenoSurface = Color(0xFF1B2435)          // Elevated card and container surface
val AdvenoSurfaceVariant = Color(0xFF243048)   // Higher elevation or hover/focus card surface

// Semantic Status Colors
val AdvenoSuccess = Color(0xFF22C55E)          // Emerald green (Online / Connected)
val AdvenoWarning = Color(0xFFF59E0B)          // Amber (Syncing / Reconnecting)
val AdvenoError = Color(0xFFEF4444)            // Coral red (Offline / Error)

// Typography & Content Colors
val AdvenoTextPrimary = Color(0xFFFFFFFF)      // Pure crisp white for primary readability
val AdvenoTextSecondary = Color(0xFFA0A8B8)    // Soft slate for subtitles, labels & metadata
val AdvenoTextMuted = Color(0xFF64748B)        // Muted gray for inactive or subtle hints

// TV Focus State Colors
val AdvenoFocusBorder = Color(0xFF22D3EE)      // High-contrast cyan border for remote focus
val AdvenoFocusGlow = Color(0x6622D3EE)        // Soft cyan halo glow
val AdvenoButtonFocused = Color(0xFF3478F6)    // Vibrant primary fill on focus
val AdvenoButtonUnfocused = Color(0xFF1B2435)  // Subtle surface fill when not focused
