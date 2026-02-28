package io.github.krozov.detekt.koin.util

import io.gitlab.arturbosch.detekt.api.Config

/**
 * Reads a config value with optional backward-compatible key migration.
 *
 * When [deprecatedKey] is provided and the old key is present in config while [key] is absent,
 * returns the old value and logs a migration warning to stderr.
 *
 * ## Usage
 *
 * New parameter (no previous key):
 * ```kotlin
 * private val myParam: List<String> =
 *     config.value(key = "myParam", default = emptyList())
 * ```
 *
 * Phase 1 — renaming "oldParam" to "newParam" in a minor release:
 * ```kotlin
 * private val newParam: List<String> = config.value(
 *     key = "newParam",
 *     default = emptyList(),
 *     deprecatedKey = "oldParam"   // reads old key with a warning
 * )
 * ```
 *
 * Phase 2 — next major release, remove [deprecatedKey]:
 * ```kotlin
 * private val newParam: List<String> =
 *     config.value(key = "newParam", default = emptyList())
 * ```
 */
internal fun <T : Any> Config.value(
    key: String,
    default: T,
    deprecatedKey: String? = null,
): T {
    if (deprecatedKey != null) {
        val newValue = valueOrNull<T>(key)
        if (newValue != null) return newValue

        val oldValue = valueOrNull<T>(deprecatedKey)
        if (oldValue != null) {
            System.err.println(
                "[detekt-rules-koin] Deprecated config key '$deprecatedKey' detected in rule config. " +
                    "Please rename it to '$key' in your detekt configuration file."
            )
            return oldValue
        }
        return default
    }

    return valueOrDefault(key, default)
}
