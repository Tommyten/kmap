rootProject.name = "kmap"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":processor")
include(":runtime")
include(":sample")

include(":multi-module-sample:moduleA")
include(":multi-module-sample:moduleB")