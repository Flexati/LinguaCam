package com.linguacam.data.repository

import android.app.Activity
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class SubscriptionPlan { FREE, PRO }

data class SubscriptionState(
    val plan: SubscriptionPlan = SubscriptionPlan.FREE,
    val isPurchased: Boolean = false,
    val purchaseDate: Long? = null,
    val unlimitedLanguages: Boolean = false,
    val hasTranslationHistory: Boolean = false,
    val hasConversationMode: Boolean = false,
    val productDetails: ProductDetails? = null,
    val isReady: Boolean = false
)

sealed class BillingFlowResult {
    object Idle : BillingFlowResult()
    object LaunchedFlow : BillingFlowResult()
    object UserCancelled : BillingFlowResult()
    /** GIR7: acquisto in stato PENDING (pagamento in sospeso, es. paesi slow-pay). */
    object PendingPurchase : BillingFlowResult()
    data class Success(val purchaseState: Int) : BillingFlowResult()
    data class Error(val responseCode: Int, val message: String) : BillingFlowResult()
}

/**
 * Interfaccia pubblica del BillingRepository.
 * Permette alla presentation di non dipendere dalla concreta BillingClient.
 * Mantiene i tipi di com.android.billingclient.api fuori dalla UI tramite BillingFlowResult.
 */
interface BillingRepositoryAPI {
    val subscriptionState: StateFlow<SubscriptionState>
    val flowResult: SharedFlow<BillingFlowResult>

    fun startConnection()

    /** Chiamato dalla UI: fornisce il ProductDetails da passare al billing flow. */
    fun setLaunchHandler(handler: ((ProductDetails) -> Unit)?)

    suspend fun restore(): Result<Unit>

    /** Avvia il flusso con i parametri costruiti dalla UI (deve chiamare BillingClient.launchBillingFlow). */
    fun launchBillingFlow(activity: Activity, params: BillingFlowParams)

    /** Richiesta di acquisto innescata dal bottone "Acquista". */
    fun requestPurchase()
}

object BillingClientFactory {
    fun create(context: android.content.Context): BillingRepositoryAPI =
        BillingRepository(context.applicationContext)
}

/**
 * Fix P1-8: prezzo mostrato in UI centralizzato in un unico object config.
 * Verrà poi sostituito dal ProductDetails.oneTimePurchaseOfferDetails una volta
 * disponibile la config server-side. Esposto come object per permettere l'import
 * da qualsiasi modulo senza duplicazione.
 */
object BillingConfig {
    const val PRO_PLAN_PRICE_EUR: Double = 4.99
}

/**
 * Fix P1-8: alias top-level backward-compat per il prezzo.
 * Mantenuto per non rompere i call-site esistenti (es. ProPlanScreen.kt).
 * Deprecato: importare direttamente `BillingConfig.PRO_PLAN_PRICE_EUR` nei nuovi file.
 */
@Suppress("ConstPropertyName")
@Deprecated(
    message = "Use BillingConfig.PRO_PLAN_PRICE_EUR for the canonical source.",
    replaceWith = ReplaceWith("BillingConfig.PRO_PLAN_PRICE_EUR")
)
const val PRO_PLAN_PRICE_EUR: Double = BillingConfig.PRO_PLAN_PRICE_EUR

/**
 * Mapping helper: stato Purchase di Google -> verso tipi interni.
 * Esposto per evitare che la UI conosca l'enum Google.
 */
object PurchaseStateMapper {
    fun toInternal(state: Int): String = when (state) {
        Purchase.PurchaseState.PURCHASED -> "PURCHASED"
        Purchase.PurchaseState.PENDING -> "PENDING"
        else -> "UNSPECIFIED"
    }
}
