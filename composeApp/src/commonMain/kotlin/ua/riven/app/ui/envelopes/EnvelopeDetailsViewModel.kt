package ua.riven.app.ui.envelopes

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.riven.app.data.FinanceRepository
import ua.riven.app.db.Envelope
import ua.riven.app.db.TransactionRecord

data class EnvelopeDetailsState(
    val envelope: Envelope? = null,
    val transactions: List<TransactionRecord> = emptyList()
)

class EnvelopeDetailsViewModel(
    private val repository: FinanceRepository,
    private val envelopeId: Long
) : ScreenModel {

    val state: StateFlow<EnvelopeDetailsState> = combine(
        repository.getEnvelopeById(envelopeId),
        repository.getTransactionsForEnvelope(envelopeId)
    ) { envelope, transactions ->
        EnvelopeDetailsState(
            envelope = envelope,
            transactions = transactions
        )
    }.stateIn(
        scope = screenModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EnvelopeDetailsState()
    )

    fun onUpdateLimit(newLimit: Double) {
        screenModelScope.launch {
            repository.updateEnvelopeLimit(envelopeId, newLimit)
        }
    }

    fun onDeleteEnvelope() {
        screenModelScope.launch {
            repository.deleteEnvelope(envelopeId)
        }
    }
}
