package ua.riven.app.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ua.riven.app.db.RivenDatabase

val androidModule = module {
    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = RivenDatabase.Schema,
            context = androidContext(),
            name = "riven.db"
        )
    }
}
