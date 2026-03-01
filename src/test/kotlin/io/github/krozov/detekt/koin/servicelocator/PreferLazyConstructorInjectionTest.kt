package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PreferLazyConstructorInjectionTest {

    @Nested
    inner class PassiveMode {

        @Test
        fun `does not flag anything when both lazyTypes and checkAllTypes are not configured`() {
            val code = """
                class MyRepo(private val db: DatabaseClient)
            """.trimIndent()

            val findings = PreferLazyConstructorInjection(Config.empty).lint(code)

            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not flag when lazyTypes is empty list and checkAllTypes is false`() {
            val config = TestConfig(
                "checkAllTypes" to false,
                "lazyTypes" to emptyList<String>()
            )

            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(private val db: DatabaseClient)
            """.trimIndent())

            assertThat(findings).isEmpty()
        }
    }
}
