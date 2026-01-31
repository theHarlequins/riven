package ua.riven.app.ui.exchange

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.riven.app.data.FinanceRepository
import ua.riven.app.db.Wallet

data class ExchangeState(
    val wallets: List<Wallet> = emptyList(),
    val fromWallet: Wallet? = null,
    val toWallet: Wallet? = null,
    val fromAmount: String = "0",
    val toAmount: Double = 0.0,
    val rate: Double = 1.0,
    val isSuccess: Boolean = false
)

class ExchangeViewModel(
    private val repository: FinanceRepository
) : ScreenModel {

    private val _fromWallet = MutableStateFlow<Wallet?>(null)
    private val _toWallet = MutableStateFlow<Wallet?>(null)
    private val _fromAmount = MutableStateFlow("0")
    private val _isSuccess = MutableStateFlow(false)

    val state: StateFlow<ExchangeState> = combine(
        repository.wallets,
        _fromWallet,
        _toWallet,
        _fromAmount,
        repository.effectiveUsdRate,
        _isSuccess
    ) { params ->
        val wallets = params[0] as List<Wallet>
        val fromW = params[1] as Wallet? ?: wallets.firstOrNull()
        val toW = params[2] as Wallet? ?: wallets.getOrNull(1) ?: wallets.firstOrNull()
        val amountStr = params[3] as String
        val usdRate = params[4] as Double
        val success = params[5] as Boolean

        val amount = amountStr.toDoubleOrNull() ?: 0.0
        
        // Calculate rate and toAmount
        // This is a simplified logic. In a real app we'd have a full matrix.
        val rate = calculateRate(fromW?.currency ?: "UAH", toW?.currency ?: "UAH", usdRate)
        val toAmount = amount * rate

        ExchangeState(
            wallets = wallets,
            fromWallet = fromW,
            toWallet = toW,
            fromAmount = amountStr,
            toAmount = toAmount,
            rate = rate,
            isSuccess = success
        )
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), ExchangeState())

    private fun calculateRate(from: String, to: String, usdRate: Double): Double {
        if (from == to) return 1.0
        
        // Convert from -> UAH
        val fromInUah = when(from) {
            "UAH" -> 1.0
            "USD" -> usdRate
            "EUR" -> usdRate * 1.08 // roughly
            "BTC" -> 3804200.0
            else -> 1.0
        }

        // Convert UAH -> to
        val uahInTo = when(to) {
            "UAH" -> 1.0
            "USD" -> 1.0 / usdRate
            "EUR" -> 1.0 / (usdRate * 1.08)
            "BTC" -> 1.0 / 3804200.0
            else -> 1.0
        }

        return fromInUah * uahInTo
    }

    fun onNumberClick(char: String) {
        val current = _fromAmount.value
        if (char == "." && current.contains(".")) return
        if (current == "0" && char != ".") {
            _fromAmount.value = char
        } else {
            _fromAmount.value = current + char
        }
    }

    fun onBackspaceClick() {
        val current = _fromAmount.value
        if (current.length <= 1) {
            _fromAmount.value = "0"
        } else {
            _fromAmount.value = current.dropLast(1)
        }
    }

    fun onFromWalletSelect(wallet: Wallet) {
        _fromWallet.value = wallet
    }

    fun onToWalletSelect(wallet: Wallet) {
        _toWallet.value = wallet
    }

    fun onExchange() {
        val s = state.value
        val fromW = s.fromWallet ?: return
        val toW = s.toWallet ?: return
        val amount = s.fromAmount.toDoubleOrNull() ?: 0.0
        
        if (amount <= 0 || fromW.id == toW.id) return

        screenModelScope.launch {
            repository.performExchange(
                fromWalletId = fromW.id,
                toWalletId = toW.id,
                fromAmount = amount,
                toAmount = s.toAmount
            )
            _isSuccess.value = true
        }
    }
}
