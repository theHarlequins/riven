package ua.riven.app.ui.dashboard

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.riven.app.data.FinanceRepository
import ua.riven.app.data.api.CurrencyApi
import ua.riven.app.db.Wallet
import ua.riven.app.db.TransactionRecord

// ==================== EVENTS ====================
sealed class DashboardEvent {
    data object Refresh : DashboardEvent()
    data class DeleteWallet(val walletId: Long) : DashboardEvent()
    data class DeleteEnvelope(val envelopeId: Long) : DashboardEvent()
    data object InjectDemoData : DashboardEvent()
}

// ==================== STATE ====================
data class DashboardState(
    val totalNetWorth: Double = 0.0,
    val totalBalance: Double = 0.0, // Sum of all wallet balances in their native currencies
    val wallets: List<Wallet> = emptyList(),
    val recentTransactions: List<TransactionRecord> = emptyList(),
    val currentUsdRate: Double = 42.0,
    val isAddWalletSheetVisible: Boolean = false,
    val isAddEnvelopeSheetVisible: Boolean = false,
    val lastUpdated: Long? = null,
    val runwayMonths: Double = 0.0,
    val monthlyBurnRate: Double = 30000.0,
    val isBurnRateDialogVisible: Boolean = false,
    val isLoading: Boolean = false
)

// ==================== VIEWMODEL ====================
class DashboardViewModel(
    private val repository: FinanceRepository,
    private val api: CurrencyApi
) : ScreenModel {

    private val isAddWalletSheetVisible = MutableStateFlow(false)
    private val isAddEnvelopeSheetVisible = MutableStateFlow(false)
    private val isBurnRateDialogVisible = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)

    init {
        loadData()
    }

    /**
     * Load all initial data - called from init and on refresh.
     */
    fun loadData() {
        screenModelScope.launch {
            _isLoading.value = true
            fetchRealRates()
            _isLoading.value = false
        }
    }

    private fun fetchRealRates() {
        screenModelScope.launch {
            try {
                val rates = api.fetchRates()
                // Monobank codes: 840 (USD), 978 (EUR), 980 (UAH)
                val usdRate = rates.find { it.currencyCodeA == 840 && it.currencyCodeB == 980 }
                val eurRate = rates.find { it.currencyCodeA == 978 && it.currencyCodeB == 980 }
                
                if (usdRate != null && eurRate != null) {
                    repository.updateRates(
                        usd = usdRate.rateSell ?: usdRate.rateCross ?: 42.0,
                        eur = eurRate.rateSell ?: eurRate.rateCross ?: 45.0
                    )
                }
            } catch (e: Exception) {
                // Fallback is implicit via repository's DB/hardcoded defaults
                e.printStackTrace()
            }
        }
    }

    // Combined financial state
    private val financeState = combine(
        repository.totalNetWorthInUah,
        repository.wallets,
        repository.effectiveUsdRate,
        repository.lastUpdated,
        repository.financialRunwayMonths,
        repository.allTransactions.map { list: List<TransactionRecord> -> list.take(5) }
    ) { params ->
        val netWorth = params[0] as Double
        val wallets = params[1] as List<Wallet>
        val rate = params[2] as Double
        val updated = params[3] as Long?
        val runway = params[4] as Double
        val transactions = params[5] as List<TransactionRecord>
        
        // Calculate total balance in native currencies
        val totalNativeBalance = wallets.sumOf { it.balance }
        
        listOf(netWorth, totalNativeBalance, wallets, rate, updated, runway, transactions)
    }

    val state: StateFlow<DashboardState> = combine(
        financeState,
        repository.monthlyBurnRate,
        isAddWalletSheetVisible,
        isAddEnvelopeSheetVisible,
        isBurnRateDialogVisible,
        _isLoading
    ) { params ->
        val finance = params[0] as List<Any?>
        val burnRate = params[1] as Double
        val isSheetVisible = params[2] as Boolean
        val isEnvelopeVisible = params[3] as Boolean
        val isDialogVisible = params[4] as Boolean
        val isLoading = params[5] as Boolean
        
        DashboardState(
            totalNetWorth = finance[0] as Double,
            totalBalance = finance[1] as Double,
            wallets = finance[2] as List<Wallet>,
            currentUsdRate = finance[3] as Double,
            lastUpdated = finance[4] as Long?,
            runwayMonths = finance[5] as Double,
            recentTransactions = finance[6] as List<TransactionRecord>,
            monthlyBurnRate = burnRate,
            isAddWalletSheetVisible = isSheetVisible,
            isAddEnvelopeSheetVisible = isEnvelopeVisible,
            isBurnRateDialogVisible = isDialogVisible,
            isLoading = isLoading
        )
    }.stateIn(
        scope = screenModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    // ==================== EVENT HANDLER ====================
    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.Refresh -> loadData()
            is DashboardEvent.DeleteWallet -> {
                screenModelScope.launch {
                    repository.deleteWallet(event.walletId)
                }
            }
            is DashboardEvent.DeleteEnvelope -> {
                screenModelScope.launch {
                    repository.deleteEnvelope(event.envelopeId)
                }
            }
            is DashboardEvent.InjectDemoData -> {
                screenModelScope.launch {
                    repository.injectDemoData()
                }
            }
        }
    }

    // ==================== UI ACTIONS ====================

    fun onRateSliderChanged(newRate: Double) {
        repository.simulateCrisisMode(newRate)
    }

    fun onAddWalletClicked() {
        isAddWalletSheetVisible.value = true
    }

    fun onAddWalletDismissed() {
        isAddWalletSheetVisible.value = false
    }

    fun onAddEnvelopeClicked() {
        isAddEnvelopeSheetVisible.value = true
    }

    fun onAddEnvelopeDismissed() {
        isAddEnvelopeSheetVisible.value = false
    }

    fun onBurnRateClicked() {
        isBurnRateDialogVisible.value = true
    }

    fun onBurnRateDismissed() {
        isBurnRateDialogVisible.value = false
    }

    fun onSaveBurnRate(amount: Double) {
        screenModelScope.launch {
            repository.updateMonthlyBurnRate(amount)
            isBurnRateDialogVisible.value = false
        }
    }

    fun onSaveWallet(name: String, currencySymbol: String, balance: Double, colorHex: String) {
        val currencyCode = when (currencySymbol) {
            "₴" -> "UAH"
            "$" -> "USD"
            "€" -> "EUR"
            "₿" -> "BTC"
            else -> "UAH"
        }
        screenModelScope.launch {
            repository.addWallet(name, currencyCode, balance, "Manual", colorHex)
            isAddWalletSheetVisible.value = false
        }
    }

    fun onSaveEnvelope(name: String, limit: Double, icon: String, colorHex: String) {
        screenModelScope.launch {
            repository.addEnvelope(name, limit, icon, colorHex)
            isAddEnvelopeSheetVisible.value = false
        }
    }

    fun onInjectDemoData() {
        onEvent(DashboardEvent.InjectDemoData)
    }

    // Overload for AddEnvelopeSheet which only provides name and limit
    fun onSaveEnvelope(name: String, limit: Double) {
        onSaveEnvelope(name, limit, "📦", "#7F5AF0")
    }
}
