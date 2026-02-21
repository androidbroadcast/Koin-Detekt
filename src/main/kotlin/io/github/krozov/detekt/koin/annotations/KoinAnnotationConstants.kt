package io.github.krozov.detekt.koin.annotations

/**
 * Shared annotation name constants used across Koin annotation rules.
 */
internal object KoinAnnotationConstants {
    /**
     * Annotations that define Koin component instances.
     *
     * Includes core definition annotations (Single, Factory, Scoped, KoinViewModel, KoinWorker)
     * and Koin Android scope qualifier annotations (ActivityScope, FragmentScope, ViewModelScope,
     * RequestScope) that mark provider functions inside @Module classes.
     */
    val DEFINITION_ANNOTATIONS: Set<String> = setOf(
        "Single", "Factory", "Scoped", "KoinViewModel", "KoinWorker",
        // Koin Android scope qualifiers — also mark provider functions inside @Module classes
        "ActivityScope", "FragmentScope", "ViewModelScope", "RequestScope"
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
