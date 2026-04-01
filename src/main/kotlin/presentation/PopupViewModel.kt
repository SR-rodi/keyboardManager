package presentation

import data.source.GlobalHotkeyManager
import domain.model.ClipboardEntry
import domain.usecase.FilterHistoryUseCase
import domain.usecase.ObserveClipboardUseCase
import domain.usecase.PasteEntryUseCase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// На desktop нет Android-lifecycle, поэтому используем обычный класс с инжектируемым скоупом.
// ViewModel как базовый класс не нужен: скоуп приложения выполняет ту же роль.
class PopupViewModel(
    private val observeClipboard: ObserveClipboardUseCase,
    private val pasteEntry: PasteEntryUseCase,
    private val filterHistory: FilterHistoryUseCase,
    private val hotkeyManager: GlobalHotkeyManager,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(PopupUiState())
    val uiState: StateFlow<PopupUiState> = _uiState.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _uiState.update { it.copy(error = throwable.message) }
    }

    init {
        // Подписываемся на историю буфера и перестраиваем фильтр при каждом изменении
        observeClipboard()
            .onEach { entries ->
                _uiState.update { state ->
                    val filtered = filterHistory(entries, state.query)
                    state.copy(history = entries, filteredHistory = filtered)
                }
            }
            .launchIn(scope)

        // Подписываемся на глобальный хоткей — показываем попап
        hotkeyManager.hotkeyEvents
            .onEach { showPopup() }
            .launchIn(scope)
    }

    fun onQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = filterHistory(state.history, query)
            state.copy(query = query, filteredHistory = filtered, selectedIndex = 0)
        }
    }

    fun onEntrySelected(entry: ClipboardEntry) {
        onDismiss()
        scope.launch(exceptionHandler) {
            pasteEntry(entry)
                .onFailure { e ->
                    _uiState.update { it.copy(error = "Ошибка вставки: ${e.message}") }
                }
        }
    }

    fun onDismiss() {
        _uiState.update {
            it.copy(isVisible = false, query = "", error = null, selectedIndex = 0)
        }
        val entries = _uiState.value.history
        _uiState.update { it.copy(filteredHistory = filterHistory(entries, "")) }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }

    fun onSelectionUp() {
        _uiState.update { state ->
            state.copy(selectedIndex = (state.selectedIndex - 1).coerceAtLeast(0))
        }
    }

    fun onSelectionDown() {
        _uiState.update { state ->
            val maxIndex = (state.filteredHistory.size - 1).coerceAtLeast(0)
            state.copy(selectedIndex = (state.selectedIndex + 1).coerceAtMost(maxIndex))
        }
    }

    private fun showPopup() {
        _uiState.update { it.copy(isVisible = true, selectedIndex = 0) }
    }
}
