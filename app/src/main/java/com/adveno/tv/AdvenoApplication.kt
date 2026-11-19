package com.adveno.tv

import android.app.Application
import com.adveno.tv.core.di.AppContainer

/**
 * Custom Application entry point holding the dependency injection [AppContainer].
 */
class AdvenoApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

