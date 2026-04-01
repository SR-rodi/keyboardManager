package data.repository

import domain.model.ClipboardEntry
import domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ClipboardRepositoryImpl : ClipboardRepository {

    private val _history = MutableStateFlow<List<ClipboardEntry>>(emptyList())
    override val history: StateFlow<List<ClipboardEntry>> = _history.asStateFlow()

    // MutableStateFlow.update использует CAS — потокобезопасен без внешней синхронизации
    override suspend fun addEntry(text: String) {
        _history.update { current ->
            val newEntry = ClipboardEntry(text = text)
            (listOf(newEntry) + current).take(MAX_ENTRIES)
        }
    }

    override fun clear() {
        _history.value = emptyList()
    }

    companion object {
        private const val MAX_ENTRIES = 50
    }
}
