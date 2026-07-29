package com.linguacam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linguacam.presentation.LocalBillingRepository
import com.linguacam.presentation.screen.MainScreen
import com.linguacam.presentation.viewmodel.MainViewModelFactory
import com.linguacam.ui.theme.LinguaCamTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val billingRepo = (application as LinguaCamApp).billingRepository

        setContent {
            LinguaCamTheme {
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
