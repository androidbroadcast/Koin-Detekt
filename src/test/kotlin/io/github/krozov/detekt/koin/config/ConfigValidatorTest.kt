package io.github.krozov.detekt.koin.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConfigValidatorTest {

    @Test
    fun `validates list configuration`() {
        val result = ConfigValidator.validateList(
            configKey = "restrictedLayers",
            value = listOf("domain", "core"),
            required = true
        )

        assertThat(result.isValid).isTrue()
    }

    @Test
    fun `reports error when required list is missing`() {
        val result = ConfigValidator.validateList(
            configKey = "restrictedLayers",
            value = null,
            required = true
        )

        assertThat(result.isValid).isFalse()
        assertThat(result.message).contains("restrictedLayers is required")
    }

    @Test
    fun `reports error when list has wrong type`() {
        // Simulates: restrictedLayers: "domain" (should be list)
        val result = ConfigValidator.validateList(
            configKey = "restrictedLayers",
            value = "domain",  // Wrong type!
            required = true
        )

        assertThat(result.isValid).isFalse()
        assertThat(result.message).contains("must be a list")
    }

    @Test
    fun `warns when list is empty`() {
        val result = ConfigValidator.validateList(
            configKey = "restrictedLayers",
            value = emptyList<String>(),
            required = false,
            warnIfEmpty = true
        )

        assertThat(result.isValid).isTrue()
        assertThat(result.warnings).containsExactly("restrictedLayers is empty, rule will be inactive")
    }
}
