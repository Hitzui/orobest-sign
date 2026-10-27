import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipFile

@CacheableTask
abstract class PrepareTwainLibrary : DefaultTask() {
    @get:Input abstract val libraryVersion: Property<String>
    @get:Input abstract val archiveSha256: Property<String>
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun prepare() {
        val version = libraryVersion.get()
        val archive = temporaryDir.resolve("ntwain.nupkg")
        val connection = URI("https://api.nuget.org/v3-flatcontainer/ntwain/$version/ntwain.$version.nupkg").toURL().openConnection().apply {
            connectTimeout = 30_000
            readTimeout = 60_000
        }
        connection.getInputStream().use { input -> archive.outputStream().use { input.copyTo(it) } }
        val hash = MessageDigest.getInstance("SHA-256").digest(archive.readBytes()).joinToString("") { "%02x".format(it) }
        check(hash.equals(archiveSha256.get(), ignoreCase = true)) { "NTwain archive checksum mismatch" }
        val target = outputDirectory.file("scanner/NTwain.dll").get().asFile
        target.parentFile.mkdirs()
        ZipFile(archive).use { zip ->
            val entry = checkNotNull(zip.getEntry("lib/net462/NTwain.dll"))
            zip.getInputStream(entry).use { input -> target.outputStream().use { input.copyTo(it) } }
        }
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

val prepareTwainLibrary = tasks.register<PrepareTwainLibrary>("prepareTwainLibrary") {
    libraryVersion.set(libs.versions.ntwain)
    archiveSha256.set("a28e17d600b345e1a387166f3b1d1913cc6b7fe0eb6c5cb572389d1519ca88ea")
    outputDirectory.set(layout.buildDirectory.dir("generated/twainResources"))
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvm()

    android {
        namespace = "com.jomiro.app.orobest_sign.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        jvmMain {
            resources.srcDir(prepareTwainLibrary.flatMap { it.outputDirectory })
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
            implementation(libs.composables.ui)
            implementation(libs.composables.icons.lucide)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
