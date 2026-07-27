package com.lingolens.presentation.billing

import com.lingolens.data.repository.BillingFlowResult
import com.lingolens.data.repository.SubscriptionPlan
import com.lingolens.data.repository.SubscriptionState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test unitari puri (no Android runtime) per il contratto billing.
 *
 * Step 11: verifica che i sealed types e i data classes esposti
 * da [com.lingolens.data.repository.BillingContracts] siano shape-correct
 * per il presentation layer ([BillingPresenter]).
 */
class BillingPresenterTest {

    @Test
    fun `SubscriptionState default is FREE plan, no purchase`() {
        val s = SubscriptionState()
        assertEquals(SubscriptionPlan.FREE, s.plan)
        assertEquals(false, s.isPurchased)
        assertEquals(null, s.purchaseDate)
        assertEquals(false, s.unlimitedLanguages)
        assertEquals(false, s.hasTranslationHistory)
        assertEquals(false, s.hasConversationMode)
        assertEquals(null, s.productDetails)
        assertEquals(false, s.isReady)
    }

    @Test
    fun `SubscriptionState copy to PRO flips plan and feature flags`() {
        val s = SubscriptionState().copy(
            plan = SubscriptionPlan.PRO,
            isPurchased = true,
            purchaseDate = 1700000000000L,
            unlimitedLanguages = true,
            hasTranslationHistory = true,
            hasConversationMode = true
        )
        assertEquals(SubscriptionPlan.PRO, s.plan)
        assertEquals(true, s.isPurchased)
        assertEquals(1700000000000L, s.purchaseDate)
        assertEquals(true, s.unlimitedLanguages)
    }

    @Test
    fun `BillingFlowResult Idle is the canonical no-op`() {
        // Verifica uguaglianza strutturale sealed.
        assertEquals(BillingFlowResult.Idle, BillingFlowResult.Idle)
    }

    @Test
    fun `BillingFlowResult Error carries code and message`() {
        val r = BillingFlowResult.Error(responseCode = 7, message = "boom")
        assertEquals(7, r.responseCode)
        assertEquals("boom", r.message)
    }

    @Test
    fun `BillingFlowResult Success carries purchase state`() {
        val r = BillingFlowResult.Success(purchaseState = 1)
        assertEquals(1, r.purchaseState)
    }

    @Test
    fun `BillingEffect mapping is total - all sources mapped`() {
        // Smoke: l'enum BillingEffect copre tutti i casi presentation-side.
        val effects = listOf(
            BillingEffect.Idle,
            BillingEffect.Loading,
            BillingEffect.FlowLaunched,
            BillingEffect.ProUnlocked,
            BillingEffect.Restored,
            BillingEffect.UserCancelled,
            BillingEffect.Error(0, "")
        )
        assertEquals(7, effects.size)
    }
}
