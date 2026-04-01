package di

import data.repository.ClipboardRepositoryImpl
import data.source.ClipboardMonitor
import data.source.GlobalHotkeyManager
import domain.repository.ClipboardRepository
import domain.usecase.FilterHistoryUseCase
import domain.usecase.ObserveClipboardUseCase
import domain.usecase.PasteEntryUseCase
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module
import presentation.PopupViewModel

fun appModule(appScope: CoroutineScope) = module {
    // Скоуп приложения
    single<CoroutineScope> { appScope }

    // Data-слой
    single<ClipboardRepository> { ClipboardRepositoryImpl() }
    single { ClipboardMonitor(repository = get(), scope = get()) }
    single { GlobalHotkeyManager(scope = get()) }

    // Use cases — factory создаёт новый экземпляр при каждой инъекции
    factory { ObserveClipboardUseCase(repository = get()) }
    factory { PasteEntryUseCase() }
    factory { FilterHistoryUseCase() }

    // PopupViewModel — синглтон на desktop (нет Android-lifecycle, скоуп управляется appScope)
    single {
        PopupViewModel(
            observeClipboard = get(),
            pasteEntry = get(),
            filterHistory = get(),
            hotkeyManager = get(),
            scope = get()
        )
    }
}
