package ua.riven.app.ui.transactions

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.riven.app.data.FinanceRepository
import ua.riven.app.db.Envelope
import ua.riven.app.db.Wallet

// ==================== TYPES ====================
enum class TransactionType {
    EXPENSE, INCOME, TRANSFER
}

sealed class TransactionEffect {
    data object NavigateBack : TransactionEffect()
    data class ShowError(val message: String) : TransactionEffect()
}

// ==================== STATE ====================
data class TransactionState(
    val amountStr: String = "0",
    val type: TransactionType = TransactionType.EXPENSE,
    val wallets: List<Wallet> = emptyList(),
    val envelopes: List<Envelope> = emptyList(),
    val selectedWallet: Wallet? = null,
    val selectedToWallet: Wallet? = null,
    val selectedEnvelope: Envelope? = null,
    val note: String = "",
    val isLoading: Boolean = false
)

// ==================== VIEWMODEL ====================
class TransactionViewModel(
    private val repository: FinanceRepository,
    private val preSelectedWalletId: Long? = null,
    private val initialType: TransactionType? = null
) : ScreenModel {

    // Calculator state
    private val _amountStr = MutableStateFlow("0")
    private val _type = MutableStateFlow(initialType ?: TransactionType.EXPENSE)
    private val _selectedWallet = MutableStateFlow<Wallet?>(null)
    private val _selectedToWallet = MutableStateFlow<Wallet?>(null)
    private val _selectedEnvelope = MutableStateFlow<Envelope?>(null)
    private val _note = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    
    // Navigation effects (one-time events)
    private val _effects = MutableSharedFlow<TransactionEffect>()
    val effects: SharedFlow<TransactionEffect> = _effects.asSharedFlow()

    val state: StateFlow<TransactionState> = combine(
        _amountStr, _type, repository.wallets, repository.envelopes,
        _selectedWallet, _selectedToWallet, _selectedEnvelope, _note, _isLoading
    ) { params ->
        val amount = params[0] as String
        val type = params[1] as TransactionType
        val wallets = params[2] as List<Wallet>
        val envelopes = params[3] as List<Envelope>
        val sw = params[4] as Wallet?
        val tw = params[5] as Wallet?
        val se = params[6] as Envelope?
        val note = params[7] as String
        val isLoading = params[8] as Boolean
        
        // Auto-select defaults
        val swFinal = sw ?: wallets.find { it.id == preSelectedWalletId } ?: wallets.firstOrNull()
        val twFinal = tw ?: wallets.getOrNull(1) ?: wallets.firstOrNull()
        val seFinal = se ?: envelopes.firstOrNull()

        TransactionState(
            amountStr = amount,
            type = type,
            wallets = wallets,
            envelopes = envelopes,
            selectedWallet = swFinal,
            selectedToWallet = twFinal,
            selectedEnvelope = seFinal,
            note = note,
            isLoading = isLoading
        )
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), TransactionState())

    // ==================== CALCULATOR INPUT HANDLERS ====================

    fun onDigitPress(digit: String) {
        val current = _amountStr.value
        
        // Prevent multiple decimal points
        if (digit == "." && current.contains(".")) return
        
        // Replace leading zero
        if (current == "0" && digit != ".") {
            _amountStr.value = digit
        } else {
            _amountStr.value = current + digit
        }
    }

    fun onBackspace() {
        val current = _amountStr.value
        if (current.length <= 1) {
            _amountStr.value = "0"
        } else {
            _amountStr.value = current.dropLast(1)
        }
    }

    fun onClear() {
        _amountStr.value = "0"
    }

    // ==================== TYPE SELECTION ====================

    fun onTypeChange(type: TransactionType) {
        _type.value = type
    }

    fun onWalletSelect(wallet: Wallet) {
        _selectedWallet.value = wallet
    }

    fun onToWalletSelect(wallet: Wallet) {
        _selectedToWallet.value = wallet
    }

    fun onEnvelopeSelect(envelope: Envelope) {
        _selectedEnvelope.value = envelope
    }

    fun onNoteChange(note: String) {
        _note.value = note
    }

    // ==================== SAVE TRANSACTION ====================

    fun onSave() {
        val amount = _amountStr.value.toDoubleOrNull()
        val wallet = _selectedWallet.value ?: state.value.wallets.firstOrNull()
        
        // Validation
        if (amount == null || amount <= 0) {
            screenModelScope.launch {
                _effects.emit(TransactionEffect.ShowError("Please enter a valid amount"))
            }
            return
        }
        
        if (wallet == null) {
            screenModelScope.launch {
                _effects.emit(TransactionEffect.ShowError("Please select a wallet"))
            }
            return
        }

        // Check for transfer requires destination wallet
        if (_type.value == TransactionType.TRANSFER) {
            val toWallet = _selectedToWallet.value
            if (toWallet == null) {
                screenModelScope.launch {
                    _effects.emit(TransactionEffect.ShowError("Please select destination wallet"))
                }
                return
            }
            if (toWallet.id == wallet.id) {
                screenModelScope.launch {
                    _effects.emit(TransactionEffect.ShowError("Cannot transfer to same wallet"))
                }
                return
            }
        }

        screenModelScope.launch {
            _isLoading.value = true
            
            try {
                when (_type.value) {
                    TransactionType.EXPENSE -> {
                        repository.addTransaction(
                            walletId = wallet.id,
                            amount = -amount, // Negative for expense
                            category = _selectedEnvelope.value?.name ?: "Expense",
                            envelopeId = _selectedEnvelope.value?.id
                        )
                    }
                    TransactionType.INCOME -> {
                        repository.addTransaction(
                            walletId = wallet.id,
                            amount = amount, // Positive for income
                            category = "Income",
                            envelopeId = null
                        )
                    }
                    TransactionType.TRANSFER -> {
                        repository.performTransfer(
                            fromWalletId = wallet.id,
                            toWalletId = _selectedToWallet.value!!.id,
                            amount = amount
                        )
                    }
                }
                
                // Emit success effect for navigation
                _effects.emit(TransactionEffect.NavigateBack)
                
            } catch (e: Exception) {
                _effects.emit(TransactionEffect.ShowError("Failed to save: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
}
