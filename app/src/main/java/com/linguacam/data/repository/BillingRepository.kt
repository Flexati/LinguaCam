package com.linguacam.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class BillingRepository(context: Context) : BillingRepositoryAPI {

    companion object {
        const val PRO_PLAN_SKU = "linguacam_pro"
        // Fix P1-8: PRO_PLAN_PRICE_EUR centralizzato in BillingConfig per evitare drift.
        // Qui riusiamo il valore esposto dal modulo contracts.
        val PRO_PLAN_PRICE_EUR: Double get() = BillingConfig.PRO_PLAN_PRICE_EUR
        // GIR5 fix MEDIO #1: MAX_RECONNECT_ATTEMPTS ora è la soglia MAX inclusiva.
        // Il loop "reconnectAttempts++ < MAX" fa MAX-1 retry prima di arrendersi.
        // Per fare 3 retry reali serve MAX_RECONNECT_ATTEMPTS = 4.
        private const val MAX_RECONNECT_ATTEMPTS = 4
        private const val MAX_ACK_RETRIES = 2
        private const val BASE_BACKOFF_MS = 500L
        private const val RESPONSE_CODE_PRODUCT_NOT_READY = -1
        private const val RESPONSE_CODE_NOT_READY = -2
        private const val RESPONSE_CODE_RESTORE_FAILED = -3
        private const val RESPONSE_CODE_ACK_FAILED = -4
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _subscriptionState = MutableStateFlow(SubscriptionState())
    override val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    private val _flowResult = MutableSharedFlow<BillingFlowResult>(extraBufferCapacity = 8)
    override val flowResult: SharedFlow<BillingFlowResult> = _flowResult.asSharedFlow()

    private var reconnectAttempts = 0
    @Volatile
    private var launchHandler: ((ProductDetails) -> Unit)? = null

    // GIR5 fix ALTO #1: Set di purchaseToken già processati per idempotency.
    // Quando lo stesso purchase arriva sia dal listener del flusso sia da restore,
    // processiamo solo la prima occorrenza e applichiamo PRO una sola volta.
    private val processedPurchaseTokens: MutableSet<String> = mutableSetOf()

    // GIR5 fix CRITICO #1: contatore retry per acknowledgePurchase (Google rimborso dopo 3gg).
    private val ackRetryCount: MutableMap<String, Int> = mutableMapOf()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase -> handlePurchase(purchase) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                appScope.launch { _flowResult.emit(BillingFlowResult.UserCancelled) }
            }
            else -> {
                appScope.launch {
                    _flowResult.emit(
                        BillingFlowResult.Error(
                            responseCode = result.responseCode,
                            message = result.debugMessage
                        )
                    )
                }
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    override fun setLaunchHandler(handler: ((ProductDetails) -> Unit)?) {
        this.launchHandler = handler
    }

    override fun startConnection() {
        if (billingClient.isReady) {
            queryProductDetails()
            appScope.launch { restoreInternal() }
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    reconnectAttempts = 0
                    queryProductDetails()
                    appScope.launch { restoreInternal() }
                } else {
                    // GIR5 fix ALTO #4: setup failure esplicita isReady=false + emette Error.
                    Timber.w("Billing setup non OK: code=${result.responseCode} msg=${result.debugMessage}")
                    _subscriptionState.update { it.copy(isReady = false) }
                    appScope.launch {
                        _flowResult.emit(
                            BillingFlowResult.Error(
                                responseCode = result.responseCode,
                                message = result.debugMessage
                            )
                        )
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    reconnectAttempts++
                    // GIR5 fix MEDIO #2: backoff esponenziale 500ms, 1s, 2s, 4s.
                    val backoff = BASE_BACKOFF_MS * (1L shl (reconnectAttempts - 1))
                    Timber.w("Billing disconnected — retry $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS dopo ${backoff}ms")
                    _subscriptionState.update { it.copy(isReady = false) }
                    appScope.launch {
                        kotlinx.coroutines.delay(backoff)
                        startConnection()
                    }
                } else {
                    Timber.e("Billing service disconnected; rinuncia dopo $reconnectAttempts tentativi")
                    _subscriptionState.update { it.copy(isReady = false) }
                    appScope.launch {
                        _flowResult.emit(
                            BillingFlowResult.Error(
                                responseCode = BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                                message = "Servizio billing non disponibile. Riavvia l'app."
                            )
                        )
                    }
                }
            }
        })
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRO_PLAN_SKU)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            ).build()
        billingClient.queryProductDetailsAsync(params) { result, list ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val pd = list.firstOrNull { it.productId == PRO_PLAN_SKU }
                _subscriptionState.update { it.copy(productDetails = pd, isReady = pd != null) }
            } else {
                Timber.w("queryProductDetails failed: ${result.debugMessage}")
                // GIR5 fix ALTO #2: failure aggiorna isReady=false per evitare stato inconsistente.
                _subscriptionState.update { it.copy(productDetails = null, isReady = false) }
                appScope.launch {
                    _flowResult.emit(
                        BillingFlowResult.Error(
                            responseCode = result.responseCode,
                            message = result.debugMessage
                        )
                    )
                }
            }
        }
    }

    override fun requestPurchase() {
        val state = _subscriptionState.value
        // GIR5 fix ALTO #3: requestPurchase controlla sia isReady sia productDetails.
        if (!state.isReady || state.productDetails == null) {
            Timber.w("requestPurchase chiamato ma isReady=${state.isReady}, productDetails=${state.productDetails != null}")
            appScope.launch {
                _flowResult.emit(
                    BillingFlowResult.Error(
                        responseCode = if (!state.isReady) RESPONSE_CODE_NOT_READY
                                       else RESPONSE_CODE_PRODUCT_NOT_READY,
                        message = "Prodotto non pronto. Attendi e riprova."
                    )
                )
            }
            return
        }
        launchHandler?.invoke(state.productDetails)
    }

    override fun launchBillingFlow(activity: Activity, params: BillingFlowParams) {
        val result = billingClient.launchBillingFlow(activity, params)
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                appScope.launch { _flowResult.emit(BillingFlowResult.LaunchedFlow) }
            }
            // GIR6 fix MEDIO: distingue USER_CANCELED da errori veri (es. BILLING_UNAVAILABLE).
            // Senza questo guard, l'utente che annulla vede un "errore" generico invece di un
            // messaggio neutrale di cancellazione.
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                appScope.launch { _flowResult.emit(BillingFlowResult.UserCancelled) }
            }
            else -> {
                appScope.launch {
                    _flowResult.emit(
                        BillingFlowResult.Error(
                            responseCode = result.responseCode,
                            message = result.debugMessage
                        )
                    )
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        // GIR5 fix CRITICO #3: verifica che il purchase contenga PRO_PLAN_SKU.
        // Senza questo check, un Purchase riforgiato (mock o test) con un altro productId
        // attiverebbe PRO senza aver pagato per il prodotto giusto.
        if (!purchase.products.contains(PRO_PLAN_SKU)) {
            Timber.w("handlePurchase: purchase ${purchase.orderId} non contiene $PRO_PLAN_SKU (ha ${purchase.products})")
            return
        }
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            // GIR7 fix: distingue PENDING (pagamento in sospeso, es. paesi slow-pay) da altri stati.
            // Su PENDING emette PendingPurchase verso la UI così l'utente vede
            // "Acquisto in elaborazione, riprova più tardi" invece di un generico "non hai comprato".
            if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                Timber.i("handlePurchase: purchase ${purchase.orderId} PENDING, in attesa pagamento")
                appScope.launch { _flowResult.emit(BillingFlowResult.PendingPurchase) }
            } else {
                Timber.w("handlePurchase: purchase ${purchase.orderId} in state ${purchase.purchaseState}, ignorato")
            }
            return
        }
        // GIR5 fix ALTO #1: idempotency via processedPurchaseTokens.
        // Lo stesso purchaseToken può arrivare sia da PurchasesUpdatedListener (acquisto live)
        // sia da queryPurchasesAsync (restore). Senza questo set, applyProState() verrebbe
        // chiamato due volte, emettendo due BillingFlowResult.Success.
        if (!processedPurchaseTokens.add(purchase.purchaseToken)) {
            Timber.d("handlePurchase: purchaseToken ${purchase.purchaseToken.take(8)}... già processato, skip")
            return
        }
        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            ackWithRetry(purchase.purchaseToken, ackParams, attempt = 0)
        } else {
            applyProState(purchase.purchaseTime)
        }
    }

    /**
     * GIR5 fix CRITICO #1: acknowledgePurchase con retry esponenziale.
     * Se ack fallisce, Google rimborsa automaticamente dopo 3 giorni.
     * Senza retry, l'utente perde PRO senza segnale. Max MAX_ACK_RETRIES tentativi
     * prima di emettere Error verso la UI.
     */
    private fun ackWithRetry(purchaseToken: String, params: AcknowledgePurchaseParams, attempt: Int) {
        billingClient.acknowledgePurchase(params) { ackResult ->
            if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                ackRetryCount.remove(purchaseToken)
                applyProState(System.currentTimeMillis())
                appScope.launch {
                    _flowResult.emit(
                        BillingFlowResult.Success(Purchase.PurchaseState.PURCHASED)
                    )
                }
            } else {
                val next = attempt + 1
                if (next < MAX_ACK_RETRIES) {
                    val backoff = BASE_BACKOFF_MS * (1L shl attempt)
                    Timber.w("Ack fallito (attempt ${attempt + 1}): code=${ackResult.responseCode}, retry in ${backoff}ms")
                    ackRetryCount[purchaseToken] = next
                    appScope.launch {
                        kotlinx.coroutines.delay(backoff)
                        ackWithRetry(purchaseToken, params, next)
                    }
                } else {
                    Timber.e("Ack fallito definitivamente dopo $MAX_ACK_RETRIES tentativi: code=${ackResult.responseCode}")
                    ackRetryCount.remove(purchaseToken)
                    appScope.launch {
                        _flowResult.emit(
                            BillingFlowResult.Error(
                                responseCode = RESPONSE_CODE_ACK_FAILED,
                                message = "Acknowledgement acquisto fallito. Google potrebbe rimborsare. Contatta supporto."
                            )
                        )
                    }
                }
            }
        }
    }

    private fun applyProState(googlePurchaseTime: Long) {
        // GIR5 fix BASSO #1: preserva purchaseDate se già impostato (es. restore successivo).
        // Usa googlePurchaseTime se disponibile, altrimenti fallback a System.currentTimeMillis().
        val resolvedDate = googlePurchaseTime.takeIf { it > 0 } ?: System.currentTimeMillis()
        _subscriptionState.update {
            it.copy(
                plan = SubscriptionPlan.PRO,
                isPurchased = true,
                purchaseDate = it.purchaseDate ?: resolvedDate,
                unlimitedLanguages = true,
                hasTranslationHistory = true,
                hasConversationMode = true
            )
        }
    }

    override suspend fun restore(): Result<Unit> {
        return try {
            val count = restoreInternal()
            // GIR5 fix ALTO #2: restore() ritorna Result.failure se 0 purchases acquistati trovati,
            // così la UI può mostrare "Nessun acquisto trovato" invece di "Restore completato".
            if (count == 0) {
                appScope.launch {
                    _flowResult.emit(
                        BillingFlowResult.Error(
                            responseCode = RESPONSE_CODE_RESTORE_FAILED,
                            message = "Nessun acquisto Pro trovato su questo account."
                        )
                    )
                }
                Result.failure(IllegalStateException("No Pro purchase found"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "restore error")
            appScope.launch {
                _flowResult.emit(
                    BillingFlowResult.Error(
                        responseCode = RESPONSE_CODE_RESTORE_FAILED,
                        message = "Ripristino fallito: ${e.message ?: "errore sconosciuto"}"
                    )
                )
            }
            Result.failure(e)
        }
    }

    private suspend fun restoreInternal(): Int {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val resultPair: Pair<com.android.billingclient.api.BillingResult, List<com.android.billingclient.api.Purchase>> =
            suspendCancellableCoroutine { cont ->
                billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                    cont.resume(billingResult to (purchases ?: emptyList()))
                }
            }
        val billingResult = resultPair.first
        val purchases = resultPair.second
        return if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            // Filtra solo purchases PURCHASED, non PENDING.
            val valid = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            valid.forEach { handlePurchase(it) }
            valid.size
        } else {
            Timber.w("queryPurchases failed: ${billingResult.debugMessage}")
            appScope.launch {
                _flowResult.emit(
                    BillingFlowResult.Error(
                        responseCode = billingResult.responseCode,
                        message = "Ripristino acquisti fallito: ${billingResult.debugMessage}"
                    )
                )
            }
            0
        }
    }

    // ============================================================
    // Legacy API mantenute per il codice pre-Step 1 (MaxInstalledLanguages etc.).
    // Non vengono rimosse in questo step: altri file potrebbero ancora chiamarle.
    // GIR2 2026-07-31: queste funzioni NON sono legacy — sono la single source of truth per il
    // free-tier 2 lingue. Sono chiamate da MainViewModel e testate in MainViewModelTest.kt:67.
    // Il commento precedente "rimosse in un refactor successivo" era misleading; rimosso.
    // ============================================================

    fun canInstallLanguageOnFreePlan(currentInstalledCount: Int): Boolean {
        return if (_subscriptionState.value.plan == SubscriptionPlan.PRO) {
            true
        } else {
            currentInstalledCount < 2
        }
    }

    fun getMaxInstalledLanguages(): Int {
        return if (_subscriptionState.value.plan == SubscriptionPlan.PRO) {
            Int.MAX_VALUE
        } else {
            2
        }
    }
}
