plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mtapk.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mtapk.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }
}

dependencies {
    // apktool-lib's nine-patch decoder uses javax.imageio/java.awt.image, which
    // don't exist on Android's runtime at all (NoClassDefFoundError as soon as a
    // 9-patch resource is decoded - practically guaranteed in any real app). The
    // original org.apktool:apktool-lib jar is excluded here and replaced with a
    // locally patched copy (app/libs/, built by stripping just that one class -
    // see README) plus its own direct dependencies re-declared explicitly so the
    // rest of the module (everything actually used) still resolves normally; this
    // app's own brut.androlib.res.decoder.ResNinePatchStreamDecoder (in
    // src/main/kotlin) fills the gap with a raw-copy implementation.
    implementation(project(":core")) {
        exclude(group = "org.apktool", module = "apktool-lib")
    }
    implementation(files("libs/apktool-lib-3.0.2-ninepatch-patched.jar"))
    implementation("org.apktool:brut.j.common:3.0.2")
    implementation("org.apktool:brut.j.util:3.0.2")
    implementation("org.apktool:brut.j.dir:3.0.2")
    implementation("org.apktool:brut.j.xml:3.0.2")
    implementation("org.apktool:brut.j.yaml:3.0.2")
    implementation("com.github.iBotPeaches.smali:smali-baksmali:b6365a84f4")
    implementation("com.github.iBotPeaches.smali:smali:b6365a84f4")
    implementation("com.google.guava:guava:33.5.0-jre")
    implementation("commons-io:commons-io:2.21.0")
    implementation("org.apache.commons:commons-text:1.15.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
