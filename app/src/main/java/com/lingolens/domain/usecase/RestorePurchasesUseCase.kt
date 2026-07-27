package com.lingolens.domain.usecase

import com.lingolens.data.repository.BillingRepositoryAPI

/**
 * UseCase per il ripristino degli acquisti da Google Play.
 */
class RestorePurchasesUseCase(private val billing: BillingRepositoryAPI) {
    suspend operator fun invoke(): Result<Unit> = billing.restore()
}
