package io.github.krozov.detekt.koin.util

internal object KoinSymbols {

    val ANNOTATION_NAMES: Set<String> = setOf(
        "Single", "Factory", "Scoped", "Module", "Qualifier",
        "KoinViewModel", "KoinWorker", "ComponentScan",
        "Configuration", "KoinApplication", "InjectedParam",
        "Property", "Scope", "ScopeId",
    )

    val DSL_NAMES: Set<String> = setOf(
        "single", "factory", "scoped", "viewModel", "worker",
        "module", "get", "getOrNull", "getAll", "inject",
    )

    val KOIN_PACKAGES: Set<String> = setOf(
        "org.koin.core.annotation",
        "org.koin.dsl",
        "org.koin.core.component",
        "org.koin.android.ext.android",
        "org.koin.androidx.viewmodel.ext.android",
        "org.koin.ktor.ext",
    )

    fun isKnownName(name: String): Boolean =
        name in ANNOTATION_NAMES || name in DSL_NAMES
}
