package ua.riven.app.ui.envelopes

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.riven.app.data.FinanceRepository
import ua.riven.app.db.Envelope

data class EnvelopesState(
    val envelopes: List<Envelope> = emptyList()
)

class EnvelopesViewModel(
    private val repository: FinanceRepository
) : ScreenModel {

    val state: StateFlow<EnvelopesState> = repository.envelopes
        .map { EnvelopesState(it) }
        .stateIn(
            scope = screenModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EnvelopesState()
        )

    fun onAddEnvelope(name: String, limit: Double, icon: String, colorHex: String) {
        screenModelScope.launch {
            repository.addEnvelope(name, limit, icon, colorHex)
        }
    }

    fun onUpdateLimit(id: Long, newLimit: Double) {
        screenModelScope.launch {
            repository.updateEnvelopeLimit(id, newLimit)
        }
    }
}
