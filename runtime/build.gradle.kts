@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

kotlin {
    abiValidation {
        enabled.set(true)
    }
}
