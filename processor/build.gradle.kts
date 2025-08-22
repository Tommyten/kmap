plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.gradleMavenPublish)
}

dependencies {
    implementation(project(":runtime"))
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}

mavenPublishing {
    coordinates(
        libs.versions.groupId.get(),
        "kmap-ksp",
        libs.versions.kmapKspVersion.get(),
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
