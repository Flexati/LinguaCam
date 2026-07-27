package com.lingolens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lingolens.presentation.LocalBillingRepository
import com.lingolens.presentation.screen.MainScreen
import com.lingolens.presentation.viewmodel.MainViewModelFactory
import com.lingolens.ui.theme.LingoLensTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val billingRepo = (application as LingoLensApp).billingRepository

        setContent {
            LingoLensTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(LocalBillingRepository provides billingRepo) {
                        MainScreen(viewModel = viewModel(factory = MainViewModelFactory(applicationContext)))
                    }
                }
            }
        }
    }
}
