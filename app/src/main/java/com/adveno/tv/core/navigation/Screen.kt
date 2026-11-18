package com.adveno.tv.core.navigation

/**
 * Defines navigation destinations within the Adveno Android TV app.
 * Using a sealed interface allows exhaustive when expressions without external routing dependencies.
 */
sealed interface Screen {
    val route: String

    data object Home : Screen {
        override val route: String = "home"
    }
}

