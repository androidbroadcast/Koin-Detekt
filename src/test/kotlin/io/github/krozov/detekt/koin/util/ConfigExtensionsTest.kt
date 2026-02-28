package io.github.krozov.detekt.koin.util

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.TestConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ConfigExtensionsTest {

    @Test
    fun `returns default when neither key nor deprecated key is present`() {
        val result = Config.empty.value(
            key = "newKey",
            default = listOf("a", "b"),
            deprecatedKey = "oldKey"
        )
        assertThat(result).isEqualTo(listOf("a", "b"))
    }

    @Test
    fun `returns new key value when present`() {
        val config = TestConfig("newKey" to listOf("x", "y"))
        val result = config.value(key = "newKey", default = emptyList<String>(), deprecatedKey = "oldKey")
        assertThat(result).isEqualTo(listOf("x", "y"))
    }

    @Test
    fun `returns old key value and prints warning when only deprecated key is present`() {
        val config = TestConfig("oldKey" to listOf("legacy"))
        val stderr = ByteArrayOutputStream()
        val original = System.err
        System.setErr(PrintStream(stderr))

        val result = config.value(key = "newKey", default = emptyList<String>(), deprecatedKey = "oldKey")

        System.setErr(original)
        assertThat(result).isEqualTo(listOf("legacy"))
        assertThat(stderr.toString()).contains("oldKey").contains("newKey")
    }

    @Test
    fun `new key takes precedence over deprecated key when both are present`() {
        val config = TestConfig("newKey" to listOf("new"), "oldKey" to listOf("old"))
        val result = config.value(key = "newKey", default = emptyList<String>(), deprecatedKey = "oldKey")
        assertThat(result).isEqualTo(listOf("new"))
    }

    @Test
    fun `returns default when key is absent and no deprecated key specified`() {
        val result = Config.empty.value(key = "myKey", default = listOf("default"))
        assertThat(result).isEqualTo(listOf("default"))
    }

    @Test
    fun `returns value when key is present and no deprecated key specified`() {
        val config = TestConfig("myKey" to listOf("value"))
        val result = config.value(key = "myKey", default = emptyList<String>())
        assertThat(result).isEqualTo(listOf("value"))
    }
}
