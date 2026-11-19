package com.adveno.tv.domain.model.screen

/**
 * Domain entity representing newly issued screen-device credentials.
 */
data class ScreenCredential(
    val screenId: String,
    val rawCredential: String
)

