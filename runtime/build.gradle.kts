@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)

    alias(libs.plugins.gradleMavenPublish)
}

kotlin {
    explicitApi()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        binaries.executable()
    }
    jvm()
    js(IR) {
//        this.nodeJs()
        this.nodejs()
        binaries.executable()
    }
    androidTarget {
        publishLibraryVariants("release", "debug")
    }
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()

    iosArm64()
    iosX64()
    iosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosX64()
    watchosSimulatorArm64()
    watchosDeviceArm64()
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()
    macosX64()
    macosArm64()

    linuxX64 {
        binaries {
            executable()
        }
    }
    linuxArm64 {
        binaries {
            executable()
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        watchosArm32(),
        watchosArm64(),
        watchosSimulatorArm64(),
        watchosDeviceArm64(),
        tvosArm64(),
        tvosX64(),
        tvosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "kmap_runtime"
        }
    }
    mingwX64()
    applyDefaultHierarchyTemplate()

    abiValidation {
        enabled.set(true)
    }
}

android {
    compileSdk = 36
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
    }
    namespace = "es.horm.kmap.runtime"
}

mavenPublishing {
    coordinates(
        groupId = libs.versions.groupId.get(),
        artifactId = "kmap-runtime",
        version = libs.versions.kmapRuntimeVersion.get()
    )

    pom {
        name = "KMap"
        description = "Mapping functions for Kotlin, made effortless"
        inceptionYear = "2025"
        url = "https://github.com/Tommyten/kmap/"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://github.com/Tommyten/kmap/blob/master/LICENSE"
            }
        }
        developers {
            developer {
                id = "Tommyten"
                name = "Thomas Hormes"
                url = "https://github.com/Tommyten/"
            }
        }
        scm {
            url = "https://github.com/Tommyten/kmap/"
            connection = "scm:git:git://github.com/Tommyten/kmap.git"
            developerConnection = "scm:git:ssh://git@github.com/Tommyten/kmap.git"
        }
    }

    publishToMavenCentral()
    signAllPublications()
}
