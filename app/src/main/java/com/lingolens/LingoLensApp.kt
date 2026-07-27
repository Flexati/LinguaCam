package com.lingolens

import android.app.Application
import com.lingolens.data.repository.BillingClientFactory
import com.lingolens.data.repository.BillingRepositoryAPI
import timber.log.Timber

/**
 * Application root: qui vivono le istanze singleton manuali (no Hilt per restare zero-budget).
 * Il BillingRepository viene creato lazy alla prima lettura (in MainActivity.onCreate).
 */
class LingoLensApp : Application() {

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
