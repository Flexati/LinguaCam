package com.lingolens.domain.usecase

import com.lingolens.data.repository.BillingRepositoryAPI

/**
 * Forza la (ri)connessione e il refresh dei ProductDetails.
 * Da invocare in Application.onCreate o al primo ingresso nella UI ProPlan.
 */
class QueryProductDetailsUseCase(private val billing: BillingRepositoryAPI) {
    operator fun invoke() {
        billing.startConnection()
    }
}
