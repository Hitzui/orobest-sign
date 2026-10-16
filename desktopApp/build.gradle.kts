import org.jetbrains.compose.desktop.application.dsl.TargetFormat

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

compose.desktop {
    application {
        mainClass = "com.jomiro.app.orobest_sign.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.jomiro.app.orobest_sign"
            packageVersion = "1.0.0"
            windows {
                iconFile.set(project.file("src/main/resources/orobest-sign.ico"))
            }
            macOS {
                iconFile.set(project.file("src/main/resources/orobest-sign.icns"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/orobest-sign.png"))
            }
        }
    }
}
