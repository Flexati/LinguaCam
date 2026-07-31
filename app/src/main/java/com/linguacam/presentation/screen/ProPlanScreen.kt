package com.linguacam.presentation.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.BillingFlowParams
import com.linguacam.data.repository.BillingRepositoryAPI
import com.linguacam.data.repository.SubscriptionPlan
import com.linguacam.data.repository.PRO_PLAN_PRICE_EUR
import com.linguacam.presentation.billing.BillingEffect
import com.linguacam.presentation.billing.BillingPresenter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Schermata di upgrade al Pro Plan.
 *
 * Wiring reale (Step 1):
 * - Riceve un BillingRepositoryAPI via parametro (DI manuale da CompositionLocal).
 * - Espone il flusso di acquisto tramite ActivityResultLauncher interno + BillingFlowParams reale.
 * - Osserva BillingEffect per gestire idle / loading / successo / errore / cancellazione.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ProPlanScreen(
    billing: BillingRepositoryAPI,
    onClose: () -> Unit,
    onPurchaseComplete: () -> Unit
) {
    val presenter = remember(billing) { BillingPresenter(billing) }
    val subscriptionState by presenter.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isPurchasing by remember { mutableStateOf(false) }
    var purchaseError by remember { mutableStateOf<String?>(null) }

    Timber.d("ProPlanScreen rendering")

    // Garantisce che il client sia connesso e productDetails in cache
    LaunchedEffect(Unit) {
        presenter.ensureReady()
    }

    // Imposta il launchHandler: la UI fornisce Activity + BillingFlowParams, il repo chiama launchBillingFlow.
    LaunchedEffect(presenter, subscriptionState.productDetails) {
        billing.setLaunchHandler { productDetails ->
            val activity = context as? Activity
            if (activity == null) {
                Timber.w("Context is not Activity; cannot launch billing flow.")
                return@setLaunchHandler
            }
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )
                ).build()
            billing.launchBillingFlow(activity, params)
        }
    }

    // Osserva gli effetti per pilotare lo stato locale
    LaunchedEffect(presenter) {
        presenter.effects.collectLatest { effect ->
            when (effect) {
                BillingEffect.Idle -> {
                    isPurchasing = false
                }
                BillingEffect.Loading -> {
                    isPurchasing = true
                }
                BillingEffect.FlowLaunched -> {
                    isPurchasing = true
                }
                BillingEffect.ProUnlocked -> {
                    isPurchasing = false
                    purchaseError = null
                    onPurchaseComplete()
                }
                BillingEffect.Restored -> {
                    isPurchasing = false
                }
                BillingEffect.UserCancelled -> {
                    isPurchasing = false
                    purchaseError = "Acquisto annullato."
                }
                BillingEffect.PendingPurchase -> {
                    isPurchasing = false
                    purchaseError = "Acquisto in elaborazione. Riceverai una notifica al completamento."
                }
                is BillingEffect.Error -> {
                    isPurchasing = false
                    purchaseError = effect.message.ifBlank { "Errore (${effect.code})" }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            TopAppBar(
                title = {
                    Text(
                        "Upgrade a Pro",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Current Plan Info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Piano attuale: ${subscriptionState.plan.name}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (subscriptionState.plan == SubscriptionPlan.FREE) {
                        Text(
                            "Max 2 lingue installate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Pro Plan Benefits
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Piano Pro",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        "€${PRO_PLAN_PRICE_EUR} una tantum",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Benefit List
                    ProBenefit(
                        icon = Icons.Default.Check,
                        title = "Lingue illimitate",
                        description = "Installa tutte le lingue supportate",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    ProBenefit(
                        icon = Icons.Default.Check,
                        title = "Cronologia traduzioni",
                        description = "Accedi alle traduzioni precedenti",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    ProBenefit(
                        icon = Icons.Default.Check,
                        title = "Modalità conversazione",
                        description = "Traduci conversazioni bidirezionali",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    ProBenefit(
                        icon = Icons.Default.Check,
                        title = "Priorità su nuove lingue",
                        description = "Accesso anticipato a nuove lingue",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Error Message
            if (purchaseError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Errore acquisto",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            purchaseError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Purchase Button
            if (subscriptionState.plan == SubscriptionPlan.FREE) {
                Button(
                    onClick = {
                        isPurchasing = true
                        presenter.purchase.requestPurchase()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    enabled = subscriptionState.isReady &&
                        subscriptionState.productDetails != null &&
                        !isPurchasing
                ) {
                    if (isPurchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            "Acquista Pro - €${PRO_PLAN_PRICE_EUR}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Acquistato",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            "Piano Pro attivo",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Restore Purchase Button
            TextButton(
                onClick = {
                    isPurchasing = true
                    scope.launch {
                        presenter.restore()
                        isPurchasing = false
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            ) {
                Text("Ripristina acquisto precedente")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ProBenefit(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier
                .size(24.dp)
                .padding(end = 12.dp)
        )

        Column {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}
