package com.linguacam

import android.app.Application
import com.linguacam.data.repository.BillingClientFactory
import com.linguacam.data.repository.BillingRepositoryAPI
import timber.log.Timber

/**
 * Application root: qui vivono le istanze singleton manuali (no Hilt per restare zero-budget).
 * Il BillingRepository viene creato lazy alla prima lettura (in MainActivity.onCreate).
 */
class LinguaCamApp : Application() {

    val billingRepository: BillingRepositoryAPI by lazy {
        BillingClientFactory.create(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
