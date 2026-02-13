package io.github.krozov.detekt.koin.config

/**
 * Validates rule configuration and provides helpful error messages.
 */
public object ConfigValidator {

    public data class ValidationResult(
        val isValid: Boolean,
        val message: String? = null,
        val warnings: List<String> = emptyList()
    )

    /**
     * Validates a list configuration parameter.
     */
    public fun validateList(
        configKey: String,
        value: Any?,
        required: Boolean = false,
        warnIfEmpty: Boolean = false
    ): ValidationResult {
        return when {
            value == null && required ->
                ValidationResult(
                    isValid = false,
                    message = "$configKey is required but not provided"
                )

            value == null && !required ->
                ValidationResult(isValid = true)

            value !is List<*> ->
                ValidationResult(
                    isValid = false,
                    message = "$configKey must be a list, got ${value!!::class.simpleName}"
                )

            value.isEmpty() && warnIfEmpty ->
                ValidationResult(
                    isValid = true,
                    warnings = listOf("$configKey is empty, rule will be inactive")
                )

            else ->
                ValidationResult(isValid = true)
        }
    }
}
