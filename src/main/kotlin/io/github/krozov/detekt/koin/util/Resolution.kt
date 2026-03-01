package io.github.krozov.detekt.koin.util

internal enum class Resolution {
    /** The name resolves to a Koin type. */
    KOIN,
    /** The name resolves to a non-Koin type. */
    NOT_KOIN,
    /** The name cannot be resolved — rule decides how to handle. */
    UNKNOWN,
}
