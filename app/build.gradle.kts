import groovy.json.JsonSlurper
import java.security.MessageDigest
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val uploadStore = providers.environmentVariable("BASAYIN_UPLOAD_KEYSTORE").orNull
val uploadPassword = providers.environmentVariable("BASAYIN_UPLOAD_PASSWORD").orNull
val uploadAlias = providers.environmentVariable("BASAYIN_UPLOAD_ALIAS").orElse("upload").get()
val uploadKeyPassword = providers.environmentVariable("BASAYIN_UPLOAD_KEY_PASSWORD").orNull ?: uploadPassword
require((uploadStore == null && uploadPassword == null) ||
    (!uploadStore.isNullOrBlank() && !uploadPassword.isNullOrBlank())) {
    "Set both BASAYIN_UPLOAD_KEYSTORE and BASAYIN_UPLOAD_PASSWORD to sign the release."
}

android {
    namespace = "app.basayin.android"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "app.basayin.android"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("basayin.versionCode").map(String::toInt).getOrElse(1)
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (uploadStore != null) {
        val uploadSigning = signingConfigs.create("upload") {
            storeFile = file(uploadStore)
            storePassword = uploadPassword
            keyAlias = uploadAlias
            keyPassword = uploadKeyPassword
        }
        buildTypes.named("release") { signingConfig = uploadSigning }
    }

    buildFeatures.compose = true
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets.named("main") {
        assets.srcDir(rootProject.layout.projectDirectory.dir("data"))
    }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

kotlin.compilerOptions.jvmTarget.set(JvmTarget.JVM_17)

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.02.01"))
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.sqlite:sqlite-bundled:2.7.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}

val database = rootProject.layout.projectDirectory.file("data/bas_ayin.sqlite")
val manifest = rootProject.layout.projectDirectory.file("data/manifest.json")
val verifyDatabase = tasks.register("verifyDatabase") {
    description = "Verifies the shared Bas Ayin database against the iOS manifest."
    group = "verification"
    inputs.files(database, manifest)
    doLast {
        val metadata = JsonSlurper().parse(manifest.asFile) as Map<*, *>
        val sha256 = MessageDigest.getInstance("SHA-256")
        database.asFile.inputStream().use { input ->
            val chunk = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(chunk)
                if (count == -1) break
                sha256.update(chunk, 0, count)
            }
        }
        val actual = sha256.digest().joinToString("") { "%02x".format(it) }
        check(actual == metadata["sha256"]) {
            "Bas Ayin data checksum mismatch. Update the database and manifest together from iOS."
        }
    }
}
tasks.named("preBuild") { dependsOn(verifyDatabase) }
