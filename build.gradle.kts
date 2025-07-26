plugins {
    kotlin("jvm") version "2.2.0" apply false
}

buildscript {
    dependencies {
        classpath(kotlin("gradle-plugin", version = "2.2.0"))
    }
}
