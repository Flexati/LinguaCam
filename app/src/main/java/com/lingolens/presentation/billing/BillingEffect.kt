package com.lingolens.presentation.billing

/**
 * Effetti esposti dalla presentation per descrivere l'esito del flusso billing.
 * Disaccoppia la UI da qualunque tipo della libreria BillingClient.
 */
sealed class BillingEffect {
    object Idle : BillingEffect()
    object Loading : BillingEffect()
    object FlowLaunched : BillingEffect()
    object ProUnlocked : BillingEffect()
    object Restored : BillingEffect()
    object UserCancelled : BillingEffect()
    data class Error(val code: Int, val message: String) : BillingEffect()
}
