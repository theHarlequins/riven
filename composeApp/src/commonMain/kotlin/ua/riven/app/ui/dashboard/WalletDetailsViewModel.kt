package ua.riven.app.ui.dashboard

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.riven.app.data.FinanceRepository
import ua.riven.app.db.TransactionRecord
import ua.riven.app.db.Wallet
import ua.riven.app.db.Envelope

data class WalletDetailsState(
    val wallet: Wallet? = null,
    val transactions: List<TransactionRecord> = emptyList(),
    val envelopes: List<Envelope> = emptyList()
)

class WalletDetailsViewModel(
    private val repository: FinanceRepository,
    private val walletId: Long
) : ScreenModel {

    val state: StateFlow<WalletDetailsState> = combine(
        repository.getWalletById(walletId),
        repository.getTransactionsForWallet(walletId),
        repository.envelopes
    ) { wallet, transactions, envelopes ->
        WalletDetailsState(
            wallet = wallet,
            transactions = transactions,
            envelopes = envelopes
        )
    }.stateIn(
        scope = screenModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WalletDetailsState()
    )

    fun onDeleteWallet() {
        screenModelScope.launch {
            repository.deleteWallet(walletId)
        }
    }
}
