package io.spixy.advancedmusicmanager.dagger

import com.activeandroid.app.Application


class BaseApplication : Application() {
    companion object {
        @JvmStatic lateinit var component: ApplicationComponent
    }

    override fun onCreate() {
        super.onCreate()
        component = DaggerApplicationComponent.builder().musicModule(MusicModule(this)).build()
    }
}