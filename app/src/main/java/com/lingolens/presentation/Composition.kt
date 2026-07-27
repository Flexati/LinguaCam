package com.lingolens.presentation

import androidx.compose.runtime.compositionLocalOf
import com.lingolens.data.repository.BillingRepositoryAPI

val LocalBillingRepository = compositionLocalOf<BillingRepositoryAPI> {
    error("BillingRepositoryAPI not provided in composition tree")
}
