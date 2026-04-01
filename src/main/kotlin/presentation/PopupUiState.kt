package presentation

import domain.model.ClipboardEntry

data class PopupUiState(
    val history: List<ClipboardEntry> = emptyList(),
    val filteredHistory: List<ClipboardEntry> = emptyList(),
    val query: String = "",
    val isVisible: Boolean = false,
    val selectedIndex: Int = 0,
    val error: String? = null
)
