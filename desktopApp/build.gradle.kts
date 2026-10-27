import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.JavaExec

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
    runtimeOnly(libs.slf4j.nop)
}

// The development run targets the local XAMPP API. Packaged applications use production HTTPS.
tasks.withType<JavaExec>().configureEach {
    if (name == "run") {
        systemProperty(
            "orobest.api.baseUrl",
            providers.gradleProperty("orobestApiBaseUrl")
                .orElse("http://localhost/orobest/api/sign/v1/").get(),
        )
    }
}

compose.desktop {
    application {
        mainClass = "com.jomiro.app.orobest_sign.MainKt"

        nativeDistributions {
            modules("java.desktop", "java.prefs")
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Orobest"
            packageVersion = "1.0.3"
            vendor = "SEIN"
            description = "Orobest: firmas de compras y documentos de clientes"
            windows {
                iconFile.set(project.file("src/main/resources/orobest-sign.ico"))
                shortcut = true
                menu = true
                menuGroup = "Orobest"
                // Keep the UpgradeCode of the original 1.0.0 MSI, even after renaming.
                upgradeUuid = "92ea12d8-d08e-31f2-a4b2-7f12adf4c1fa"
            }
            macOS {
                iconFile.set(project.file("src/main/resources/orobest-sign.icns"))
            }
            linux {
                packageName = "orobest"
                iconFile.set(project.file("src/main/resources/orobest-sign.png"))
            }
        }
    }
}
