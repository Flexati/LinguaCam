package com.linguacam.presentation.billing

import com.linguacam.data.repository.BillingFlowResult
import com.linguacam.data.repository.BillingRepositoryAPI
import com.linguacam.domain.usecase.PurchaseProPlanUseCase
import com.linguacam.domain.usecase.QueryProductDetailsUseCase
import com.linguacam.domain.usecase.RestorePurchasesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Presenter: converte BillingFlowResult esposto dal repository in BillingEffect presentation-friendly.
 */
class BillingPresenter(billing: BillingRepositoryAPI) {

    val state: StateFlow<com.linguacam.data.repository.SubscriptionState> =
        billing.subscriptionState

    val ensureReady = QueryProductDetailsUseCase(billing)
    val purchase = PurchaseProPlanUseCase(billing)
    val restore = RestorePurchasesUseCase(billing)

    val effects: Flow<BillingEffect> = billing.flowResult.map { result ->
        when (result) {
            BillingFlowResult.Idle -> BillingEffect.Idle
            BillingFlowResult.LaunchedFlow -> BillingEffect.FlowLaunched
            BillingFlowResult.UserCancelled -> BillingEffect.UserCancelled
            is BillingFlowResult.Success -> BillingEffect.ProUnlocked
            is BillingFlowResult.Error -> BillingEffect.Error(
                code = result.responseCode,
                message = result.message
            )
        }
    }
}
