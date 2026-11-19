package com.adveno.tv.domain.model.screen

/**
 * Domain entity representing a registered Adveno TV screen.
 */
data class Screen(
    val id: String,
    val screenCode: String,
    val name: String,
    val deviceModel: String? = null,
    val appVersion: String? = null,
    val status: ScreenStatus = ScreenStatus.ACTIVE,
    val credential: String? = null
)

