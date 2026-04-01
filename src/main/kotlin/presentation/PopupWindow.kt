package presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import domain.model.ClipboardEntry
import java.awt.GraphicsEnvironment
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.delay

@Composable
fun PopupWindow(
    uiState: PopupUiState,
    onQueryChange: (String) -> Unit,
    onEntrySelected: (ClipboardEntry) -> Unit,
    onDismiss: () -> Unit,
    onSelectionUp: () -> Unit,
    onSelectionDown: () -> Unit,
    onErrorDismissed: () -> Unit
) {
    if (!uiState.isVisible) return

    val screenBounds = GraphicsEnvironment
        .getLocalGraphicsEnvironment()
        .defaultScreenDevice
        .defaultConfiguration
        .bounds

    val windowWidth = 480
    val windowHeight = 420

    val windowState = rememberWindowState(
        size = DpSize(windowWidth.dp, windowHeight.dp),
        position = WindowPosition(
            x = (screenBounds.x + (screenBounds.width - windowWidth) / 2).dp,
            y = (screenBounds.y + (screenBounds.height - windowHeight) / 3).dp
        )
    )

    Window(
        onCloseRequest = onDismiss,
        state = windowState,
        title = "",
        undecorated = true,
        alwaysOnTop = true,
        resizable = false,
        focusable = true
    ) {
        // Guard: не закрывать попап по потере фокуса первые 300мс.
        // Без задержки возможна гонка: ОС присваивает фокус новому окну не мгновенно,
        // и windowLostFocus может сработать раньше, чем windowGainedFocus — попап
        // закроется сразу после открытия.
        val dismissOnFocusLoss = remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(300)
            dismissOnFocusLoss.value = true
        }

        // Закрываем попап при потере фокуса окна (клик вне окна)
        DisposableEffect(Unit) {
            val focusListener = object : WindowFocusListener {
                override fun windowGainedFocus(e: WindowEvent?) = Unit
                override fun windowLostFocus(e: WindowEvent?) {
                    if (dismissOnFocusLoss.value) onDismiss()
                }
            }
            window.addWindowFocusListener(focusListener)
            onDispose { window.removeWindowFocusListener(focusListener) }
        }

        PopupContent(
            uiState = uiState,
            onQueryChange = onQueryChange,
            onEntrySelected = onEntrySelected,
            onDismiss = onDismiss,
            onSelectionUp = onSelectionUp,
            onSelectionDown = onSelectionDown,
            onErrorDismissed = onErrorDismissed
        )
    }
}

@Composable
private fun PopupContent(
    uiState: PopupUiState,
    onQueryChange: (String) -> Unit,
    onEntrySelected: (ClipboardEntry) -> Unit,
    onDismiss: () -> Unit,
    onSelectionUp: () -> Unit,
    onSelectionDown: () -> Unit,
    onErrorDismissed: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Автопрокрутка к выбранному элементу
    LaunchedEffect(uiState.selectedIndex) {
        if (uiState.filteredHistory.isNotEmpty()) {
            listState.animateScrollToItem(uiState.selectedIndex)
        }
    }

    // Фокус на поле поиска при открытии попапа
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                            when (event.key) {
                                Key.Escape -> { onDismiss(); true }
                                Key.Enter -> {
                                    val selected = uiState.filteredHistory.getOrNull(uiState.selectedIndex)
                                    if (selected != null) { onEntrySelected(selected); true } else false
                                }
                                Key.DirectionUp -> { onSelectionUp(); true }
                                Key.DirectionDown -> { onSelectionDown(); true }
                                else -> false
                            }
                        },
                    placeholder = { Text("Поиск по истории буфера обмена...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Поиск")
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.filteredHistory.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.query.isBlank())
                                "История буфера обмена пуста"
                            else
                                "Нет результатов для «${uiState.query}»",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.filteredHistory,
                            key = { _, entry -> entry.id }
                        ) { index, entry ->
                            ClipboardEntryItem(
                                entry = entry,
                                isSelected = index == uiState.selectedIndex,
                                onClick = { onEntrySelected(entry) }
                            )
                        }
                    }
                }
            }

            // Snackbar для отображения ошибок
            uiState.error?.let { errorMessage ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Snackbar(
                        modifier = Modifier.padding(8.dp),
                        action = {
                            TextButton(onClick = onErrorDismissed) { Text("Закрыть") }
                        }
                    ) {
                        Text(errorMessage)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipboardEntryItem(
    entry: ClipboardEntry,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        Color.Transparent

    val formatter = remember { SimpleDateFormat("HH:mm:ss") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val previewText = if (entry.text.length > 80)
            entry.text.take(80) + "…"
        else
            entry.text

        Text(
            text = previewText,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatter.format(Date(entry.timestamp)),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
