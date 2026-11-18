package com.adveno.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.adveno.tv.presentation.screens.home.HomeScreen
import com.adveno.tv.presentation.screens.home.HomeViewModel
import com.adveno.tv.presentation.theme.AdvenoTheme

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdvenoTheme {
                HomeScreen(
                    viewModel = homeViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
