import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.net.URI
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

val firma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.isFile) archivo.inputStream().use(::load)
}
val firmaCompleta = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .all { !firma.getProperty(it).isNullOrBlank() }
val pharmaApiUrl = providers.gradleProperty("pharmaApiUrl")
    .orElse("http://10.0.2.2:8080/api/v1/").get()
val pharmaUri = URI(pharmaApiUrl)
require(pharmaUri.scheme in listOf("http", "https") && !pharmaUri.host.isNullOrBlank() && pharmaUri.userInfo == null) {
    "pharmaApiUrl debe ser una URL HTTP(S) sin credenciales"
}
abstract class GenerarRedPharma : DefaultTask() {
    @get:Input abstract val host: Property<String>
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty
    @TaskAction fun generar() {
        val archivo = outputDirectory.get().file("xml/pharma_network_security.xml").asFile
        archivo.parentFile.mkdirs()
        archivo.writeText("""<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">${host.get()}</domain>
    </domain-config>
</network-security-config>
""")
    }
}
val generarRedPharma = tasks.register<GenerarRedPharma>("generarRedPharma") {
    host.set(pharmaUri.host)
    outputDirectory.set(layout.buildDirectory.dir("generated/pharmaNetwork/res"))
}
androidComponents.onVariants { variant ->
    variant.sources.res?.addGeneratedSourceDirectory(generarRedPharma, GenerarRedPharma::outputDirectory)
}
abstract class ValidarFirmaRelease : DefaultTask() {
    @get:Input abstract val configurada: Property<Boolean>
    @get:Input abstract val ruta: Property<String>
    @TaskAction fun validar() {
        check(configurada.get()) { "Configura keystore.properties local con las cuatro propiedades de firma release." }
        check(File(ruta.get()).isFile) { "No existe el almacén release configurado." }
    }
}
val validarFirmaRelease = tasks.register<ValidarFirmaRelease>("validarFirmaRelease") {
    configurada.set(firmaCompleta)
    ruta.set(firma.getProperty("storeFile")?.let { rootProject.file(it).absolutePath }.orEmpty())
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(validarFirmaRelease) }

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
    androidTestImplementation(libs.ktor.client.core)
    androidTestImplementation(libs.kotlinx.coroutines.core)
    androidTestImplementation(libs.koin.core)
}

android {
    namespace = "pe.edu.upeu.pharmamobilee"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "pe.edu.upeu.pharmamobilee"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
        manifestPlaceholders["pharmaApiBaseUrl"] = pharmaApiUrl
        testInstrumentationRunner = "pe.edu.upeu.pharmamobilee.Sesion403Instrumentation"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("release") {
            if (firmaCompleta) {
                storeFile = rootProject.file(firma.getProperty("storeFile"))
                storePassword = firma.getProperty("storePassword")
                keyAlias = firma.getProperty("keyAlias")
                keyPassword = firma.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}
