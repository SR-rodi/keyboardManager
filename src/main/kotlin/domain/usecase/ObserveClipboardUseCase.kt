package domain.usecase

import domain.model.ClipboardEntry
import domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveClipboardUseCase(
    private val repository: ClipboardRepository
) {
    operator fun invoke(): StateFlow<List<ClipboardEntry>> = repository.history
}
