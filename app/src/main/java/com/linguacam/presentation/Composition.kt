package com.linguacam.presentation

import androidx.compose.runtime.compositionLocalOf
import com.linguacam.data.repository.BillingRepositoryAPI

val LocalBillingRepository = compositionLocalOf<BillingRepositoryAPI> {
    error("BillingRepositoryAPI not provided in composition tree")
}
