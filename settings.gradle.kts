rootProject.name = "detekt-koin4-rules"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        // Explicitly acknowledge version catalog usage
        // Default catalog 'libs' auto-created from gradle/libs.versions.toml
    }
}
