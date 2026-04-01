import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.application
import data.source.ClipboardMonitor
import data.source.GlobalHotkeyManager
import di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import presentation.PopupViewModel
import presentation.PopupWindow

fun main() {
    // Единый скоуп приложения: SupervisorJob — сбой одного дочернего скоупа
    // не отменяет остальных (монитор и хоткей независимы)
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    application {
        KoinApplication(application = { modules(appModule(appScope)) }) {

            val hotkeyManager = koinInject<GlobalHotkeyManager>()
            val clipboardMonitor = koinInject<ClipboardMonitor>()

            // Запускаем фоновые сервисы один раз при старте композиции
            LaunchedEffect(Unit) {
                hotkeyManager.register()
                clipboardMonitor.start()
            }

            val viewModel: PopupViewModel = koinInject()
            val uiState by viewModel.uiState.collectAsState()

            MaterialTheme {
                PopupWindow(
                    uiState = uiState,
                    onQueryChange = viewModel::onQueryChange,
                    onEntrySelected = viewModel::onEntrySelected,
                    onDismiss = viewModel::onDismiss,
                    onSelectionUp = viewModel::onSelectionUp,
                    onSelectionDown = viewModel::onSelectionDown,
                    onErrorDismissed = viewModel::onErrorDismissed
                )
            }

            // Очистка ресурсов при завершении приложения
            DisposableEffect(Unit) {
                onDispose {
                    hotkeyManager.dispose()
                    appScope.cancel()
                }
            }
        }
    }
}
