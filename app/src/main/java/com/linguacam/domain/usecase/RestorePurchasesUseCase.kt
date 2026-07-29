package com.linguacam.domain.usecase

import com.linguacam.data.repository.BillingRepositoryAPI

/**
 * UseCase per il ripristino degli acquisti da Google Play.
 */
class RestorePurchasesUseCase(private val billing: BillingRepositoryAPI) {
    suspend operator fun invoke(): Result<Unit> = billing.restore()
}
