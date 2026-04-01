package data.source

import domain.repository.ClipboardRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

class ClipboardMonitor(
    private val repository: ClipboardRepository,
    private val scope: CoroutineScope
) {
    private var lastSeenText: String? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        System.err.println("[ClipboardMonitor] Необработанное исключение: ${throwable.message}")
    }

    fun start() {
        scope.launch(exceptionHandler) {
            while (true) {
                runCatching { pollClipboard() }
                    .onFailure { e ->
                        System.err.println("[ClipboardMonitor] Ошибка опроса: ${e.message}")
                    }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun pollClipboard() = withContext(Dispatchers.IO) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard

        // isDataFlavorAvailable может бросить исключение, если буфер временно заблокирован
        if (!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) return@withContext

        val text = runCatching {
            clipboard.getData(DataFlavor.stringFlavor) as String
        }.getOrNull() ?: return@withContext

        if (text.isBlank()) return@withContext
        if (text == lastSeenText) return@withContext

        lastSeenText = text
        repository.addEntry(text)
    }

    companion object {
        private const val POLL_INTERVAL_MS = 300L
    }
}
