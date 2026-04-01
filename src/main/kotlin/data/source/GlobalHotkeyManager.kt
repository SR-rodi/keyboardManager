package data.source

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GlobalHotkeyManager(private val scope: CoroutineScope) {

    private val _hotkeyEvents = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val hotkeyEvents: SharedFlow<Unit> = _hotkeyEvents.asSharedFlow()

    // Держим сильную ссылку на коллбэк: JNA хранит коллбэки по слабой ссылке
    // и GC может удалить объект, если нет внешней ссылки, что приведёт к краху.
    @Volatile private var hookHandle: WinUser.HHOOK? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, t ->
        System.err.println("[GlobalHotkeyManager] ${t.message}")
    }

    private val hookProc = WinUser.LowLevelKeyboardProc { nCode, wParam, info ->
        if (nCode >= 0) {
            // Явно читаем поля структуры из нативной памяти
            info.read()

            val eventType = wParam.toInt()
            if (eventType == WinUser.WM_KEYDOWN || eventType == WM_SYSKEYDOWN) {
                if (info.vkCode == VK_V) {
                    val ctrlDown  = User32.INSTANCE.GetAsyncKeyState(VK_CONTROL).toInt() and 0x8000 != 0
                    val shiftDown = User32.INSTANCE.GetAsyncKeyState(VK_SHIFT).toInt()   and 0x8000 != 0

                    if (ctrlDown && shiftDown) {
                        scope.launch(exceptionHandler) { _hotkeyEvents.emit(Unit) }

                        // Возвращаем ненулевое значение — Windows не передаёт событие
                        // в очередь приложения (подавление), CallNextHookEx не вызываем.
                        return@LowLevelKeyboardProc WinDef.LRESULT(1)
                    }
                }
            }
        }

        // Событие не наше — передаём дальше по цепочке хуков
        User32.INSTANCE.CallNextHookEx(
            hookHandle,
            nCode,
            wParam,
            WinDef.LPARAM(Pointer.nativeValue(info.pointer))
        )
    }

    fun register() {
        // WH_KEYBOARD_LL требует, чтобы устанавливающий поток качал Win32-сообщения.
        // Swing EDT на Windows делает это через JNI, поэтому используем Dispatchers.Main.
        scope.launch(exceptionHandler) {
            withContext(Dispatchers.Main) {
                runCatching {
                    hookHandle = User32.INSTANCE.SetWindowsHookEx(
                        WinUser.WH_KEYBOARD_LL,
                        hookProc,
                        null,
                        0
                    ) ?: error("SetWindowsHookEx вернул null")
                }.onFailure { e ->
                    System.err.println("[GlobalHotkeyManager] Ошибка регистрации хука: ${e.message}")
                }
            }
        }
    }

    fun dispose() {
        runCatching {
            hookHandle?.let { User32.INSTANCE.UnhookWindowsHookEx(it) }
        }.onFailure { e ->
            System.err.println("[GlobalHotkeyManager] Ошибка снятия хука: ${e.message}")
        }
    }

    companion object {
        private const val VK_V       = 0x56
        private const val VK_CONTROL = 0x11
        private const val VK_SHIFT   = 0x10
        private const val WM_SYSKEYDOWN = 0x0104
    }
}
