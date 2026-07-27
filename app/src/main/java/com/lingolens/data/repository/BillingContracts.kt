package com.lingolens.data.repository

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

/** Prezzo mostrato in UI (verrà poi sostituito dal ProductDetails.oneTimePurchaseOfferDetails). */
const val PRO_PLAN_PRICE_EUR: Double = 4.99

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
