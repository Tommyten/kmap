plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":runtime"))
    ksp(project(":processor"))
}
