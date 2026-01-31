package ua.riven.app.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ua.riven.app.data.FinanceRepository
import ua.riven.app.data.api.CurrencyApi
import ua.riven.app.db.RivenDatabase
import ua.riven.app.ui.dashboard.DashboardViewModel
import ua.riven.app.ui.dashboard.WalletDetailsViewModel
import ua.riven.app.ui.envelopes.EnvelopeDetailsViewModel
import ua.riven.app.ui.envelopes.EnvelopesViewModel
import ua.riven.app.ui.exchange.ExchangeViewModel
import ua.riven.app.ui.transactions.TransactionViewModel
import ua.riven.app.ui.transactions.TransactionType

val appModule = module {
    // Network & Database
    single { CurrencyApi.createHttpClient() }
    singleOf(::CurrencyApi)
    single { RivenDatabase(get()) }
    single { FinanceRepository(get()) }

    // Dashboard
    factory { DashboardViewModel(get(), get()) }
    factory { (walletId: Long) -> WalletDetailsViewModel(get(), walletId) }

    // Envelopes
    factoryOf(::EnvelopesViewModel)
    factory { (envelopeId: Long) -> EnvelopeDetailsViewModel(get(), envelopeId) }

    // Transactions
    // Using simple getOrNull() to allow calling without parameters or with parameters
    factory { params ->
        TransactionViewModel(
            repository = get(),
            preSelectedWalletId = params.getOrNull(),
            initialType = params.getOrNull()
        )
    }

    // Exchange
    factoryOf(::ExchangeViewModel)
}
