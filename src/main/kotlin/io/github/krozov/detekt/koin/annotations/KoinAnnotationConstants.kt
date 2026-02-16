package io.github.krozov.detekt.koin.annotations

/**
 * Shared annotation name constants used across Koin annotation rules.
 */
internal object KoinAnnotationConstants {
    /** Annotations that define Koin component instances (Single, Factory, Scoped, KoinViewModel, KoinWorker). */
    val DEFINITION_ANNOTATIONS: Set<String> = setOf(
        "Single", "Factory", "Scoped", "KoinViewModel", "KoinWorker"
    )

    /** Provider-level annotations used inside @Module classes (Single, Factory, Scoped). */
    val PROVIDER_ANNOTATIONS: Set<String> = setOf(
        "Single", "Factory", "Scoped"
    )

    /** All Koin annotations including module-level and configuration annotations. */
    val ALL_ANNOTATIONS: Set<String> = setOf(
        "Single", "Factory", "Scoped", "Module",
        "KoinViewModel", "KoinWorker", "ComponentScan",
        "Configuration", "KoinApplication"
    )
}
