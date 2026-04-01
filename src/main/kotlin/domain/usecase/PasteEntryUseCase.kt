package domain.usecase

import domain.model.ClipboardEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.awt.Robot
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent

class PasteEntryUseCase {
    suspend operator fun invoke(entry: ClipboardEntry): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Кладём текст в системный буфер обмена
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(StringSelection(entry.text), null)

                // Ждём возврата фокуса в предыдущее окно после закрытия попапа.
                // 150мс — надёжный порог на Windows 10/11; 50мс часто недостаточно.
                delay(150)

                // Имитируем Ctrl+V через Robot
                val robot = Robot()
                robot.keyPress(KeyEvent.VK_CONTROL)
                robot.keyPress(KeyEvent.VK_V)
                robot.keyRelease(KeyEvent.VK_V)
                robot.keyRelease(KeyEvent.VK_CONTROL)
            }
        }
}
