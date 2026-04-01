package domain.usecase

import domain.model.ClipboardEntry

class FilterHistoryUseCase {
    operator fun invoke(
        entries: List<ClipboardEntry>,
        query: String
    ): List<ClipboardEntry> {
        if (query.isBlank()) return entries
        return entries.filter { it.text.contains(query, ignoreCase = true) }
    }
}
