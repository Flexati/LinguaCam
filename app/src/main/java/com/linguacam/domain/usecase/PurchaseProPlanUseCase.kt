package com.linguacam.domain.usecase

import com.linguacam.data.repository.BillingRepositoryAPI
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase per l'acquisto del Pro Plan.
 * Espone solo ciò che la UI deve sapere; non accede direttamente a BillingClient.
 */
class PurchaseProPlanUseCase(private val billing: BillingRepositoryAPI) {

    val state: StateFlow<com.linguacam.data.repository.SubscriptionState> =
        billing.subscriptionState

    fun ensureReady() {
        billing.startConnection()
    }

    fun requestPurchase() {
        billing.requestPurchase()
    }
}
