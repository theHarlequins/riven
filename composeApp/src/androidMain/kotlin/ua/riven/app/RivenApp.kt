package ua.riven.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import ua.riven.app.di.androidModule
import ua.riven.app.di.appModule

class RivenApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@RivenApp)
            androidLogger()
            modules(appModule, androidModule)
        }
    }
}
