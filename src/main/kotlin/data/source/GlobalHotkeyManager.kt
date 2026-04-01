package data.source

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.logging.Level
import java.util.logging.Logger

class GlobalHotkeyManager(private val scope: CoroutineScope) {

    private val _hotkeyEvents = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val hotkeyEvents: SharedFlow<Unit> = _hotkeyEvents.asSharedFlow()

    private val keyListener = object : NativeKeyListener {
        override fun nativeKeyPressed(e: NativeKeyEvent) {
            // Проверяем Ctrl+Shift+V через битовые маски (несколько модификаторов могут быть активны одновременно)
            val isCtrl = (e.modifiers and NativeKeyEvent.CTRL_MASK) != 0
            val isShift = (e.modifiers and NativeKeyEvent.SHIFT_MASK) != 0
            val isV = e.keyCode == NativeKeyEvent.VC_V

            if (isCtrl && isShift && isV) {
                // Подавляем нативное событие — без этого Ctrl+Shift+V дойдёт до
                // активного приложения (Chrome/Word воспринимают его как "вставить
                // без форматирования") и вставка произойдёт ДО появления попапа.
                runCatching { GlobalScreen.suppressEvent(e) }
                    .onFailure { ex ->
                        System.err.println("[GlobalHotkeyManager] suppressEvent failed: ${ex.message}")
                    }

                // nativeKeyPressed вызывается на нативном треде JNativeHook.
                // scope.launch переводит эмиссию в корутинный диспетчер.
                scope.launch { _hotkeyEvents.emit(Unit) }
            }
        }

        override fun nativeKeyReleased(e: NativeKeyEvent) = Unit
        override fun nativeKeyTyped(e: NativeKeyEvent) = Unit
    }

    fun register() {
        // Подавляем verbose-логирование JNativeHook до вызова registerNativeHook()
        val logger = Logger.getLogger(GlobalScreen::class.java.`package`.name)
        logger.level = Level.OFF
        logger.useParentHandlers = false

        runCatching {
            GlobalScreen.registerNativeHook()
            GlobalScreen.addNativeKeyListener(keyListener)
        }.onFailure { e ->
            System.err.println("[GlobalHotkeyManager] Не удалось зарегистрировать хук: ${e.message}")
        }
    }

    fun dispose() {
        runCatching {
            GlobalScreen.removeNativeKeyListener(keyListener)
            GlobalScreen.unregisterNativeHook()
        }.onFailure { e ->
            System.err.println("[GlobalHotkeyManager] Не удалось снять хук: ${e.message}")
        }
    }
}
