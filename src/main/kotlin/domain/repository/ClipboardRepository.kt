package domain.repository

import domain.model.ClipboardEntry
import kotlinx.coroutines.flow.StateFlow

interface ClipboardRepository {
    val history: StateFlow<List<ClipboardEntry>>
    suspend fun addEntry(text: String)
    fun clear()
}
