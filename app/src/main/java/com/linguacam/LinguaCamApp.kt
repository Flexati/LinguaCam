package com.linguacam

import android.app.Application
import com.linguacam.data.repository.BillingClientFactory
import com.linguacam.data.repository.BillingRepositoryAPI
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Application root: qui vivono le istanze singleton manuali (no Hilt per restare zero-budget).
 * Il BillingRepository viene creato lazy alla prima lettura (in MainActivity.onCreate).
 */
class LinguaCamApp : Application() {

    /**
     * Fix P1-9: la creazione di BillingClient può lanciare NoClassDefFoundError
     * o RuntimeException su device senza Google Play Services (es. emulatori
     * vanilla, AOSP, WearOS). Wrappiamo la lazy in runCatching per non crashare
     * l'app all'avvio: ritorniamo un fallback che no-op su tutte le chiamate,
     * lasciando l'app girare in modalità "billing disabilitato".
     */
    val billingRepository: BillingRepositoryAPI by lazy {
        runCatching {
            BillingClientFactory.create(applicationContext)
        }.getOrElse { error ->
            Timber.e(error, "BillingClient non disponibile (GMS mancante?). Fallback stub.")
            StubBillingRepository()
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}

/**
 * Fix P1-9: fallback no-op per quando Google Play Services non è disponibile.
 * Espone lo stesso contratto di BillingRepositoryAPI ma ignora silenziosamente
 * tutte le operazioni. Lo stato di subscription rimane FREE.
 */
private class StubBillingRepository : BillingRepositoryAPI {
    private val emptyState = kotlinx.coroutines.flow.MutableStateFlow(
        com.linguacam.data.repository.SubscriptionState()
    )
    override val subscriptionState = emptyState.asStateFlow()
    override val flowResult = kotlinx.coroutines.flow.MutableSharedFlow<com.linguacam.data.repository.BillingFlowResult>(extraBufferCapacity = 4).asSharedFlow()

    override fun startConnection() { /* no-op: GMS assente */ }
    override fun setLaunchHandler(handler: ((com.android.billingclient.api.ProductDetails) -> Unit)?) { /* no-op */ }
    override suspend fun restore(): Result<Unit> = Result.success(Unit)
    override fun launchBillingFlow(
        activity: android.app.Activity,
        params: com.android.billingclient.api.BillingFlowParams
    ) { /* no-op */ }
    override fun requestPurchase() { /* no-op */ }
}
