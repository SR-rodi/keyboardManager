import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
    id("org.jetbrains.compose") version "1.8.2"
    id("com.gradleup.shadow") version "8.3.6"
}

group = "com.keyboardmanager"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Compose Desktop runtime — currentOs pulls correct native Skia binary
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Coroutines — swing variant provides Dispatchers.Main on JVM desktop
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")

    // Koin — BOM keeps all artifacts in sync
    implementation(platform("io.insert-koin:koin-bom:4.1.1"))
    implementation("io.insert-koin:koin-core")
    implementation("io.insert-koin:koin-compose")
    implementation("io.insert-koin:koin-compose-viewmodel")

    // JNA — WH_KEYBOARD_LL хук с поддержкой подавления событий (заменяет JNativeHook)
    implementation("net.java.dev.jna:jna-platform:5.13.0")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "ClipboardManager"
            packageVersion = "1.0.0"
            windows {
                menuGroup = "ClipboardManager"
                upgradeUuid = "3f2d8c1e-4a5b-6c7d-8e9f-0a1b2c3d4e5f"
            }
        }
    }
}

tasks.shadowJar {
    archiveBaseName.set("clipboard-manager")
    archiveVersion.set("1.0.0")
    archiveClassifier.set("fat")
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    mergeServiceFiles()
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
}
