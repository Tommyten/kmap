plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("es.horm.kmap:kmap-runtime:0.1.0")
    ksp("es.horm.kmap:kmap-processor:0.1.0")
}
