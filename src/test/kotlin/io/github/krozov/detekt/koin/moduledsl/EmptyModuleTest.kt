package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EmptyModuleTest {

    @Test
    fun `reports empty module`() {
        val code = """
            import org.koin.dsl.module

            val emptyModule = module { }
        """.trimIndent()

        val findings = EmptyModule(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("Empty module")
    }

    @Test
    fun `does not report module with includes`() {
        val code = """
            import org.koin.dsl.module

            val featureModule = module {
                includes(networkModule)
            }
        """.trimIndent()

        val findings = EmptyModule(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report module with definitions`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { MyService() }
            }
        """.trimIndent()

        val findings = EmptyModule(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports module with only whitespace`() {
        val code = """
            import org.koin.dsl.module

            val m = module {


            }
        """.trimIndent()

        val findings = EmptyModule(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports module with only comments`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                // TODO: Add definitions
                /* Work in progress */
            }
        """.trimIndent()

        val findings = EmptyModule(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports module with empty nested lambda`() {
        val code = """
            import org.koin.dsl.module

            val m = module {

            }
        """.trimIndent()

        val findings = EmptyModule(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `handles module with value argument syntax`() {
        val code = """
            import org.koin.dsl.module

            val m = module({ })
        """.trimIndent()

        val findings = EmptyModule(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `handles module with value argument and definitions`() {
        val code = """
            import org.koin.dsl.module

            val m = module({ single { Service() } })
        """.trimIndent()

        val findings = EmptyModule(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `ignores non-module call expressions`() {
        val code = """
            fun myFunction() = someCall { }
        """.trimIndent()

        val findings = EmptyModule(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports with enhanced message format`() {
        val code = """
            import org.koin.dsl.module

            val m = module { }
        """.trimIndent()

        val findings = EmptyModule(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    // Edge case: Multiple empty modules in same file
    @Test
    fun `reports multiple empty modules in same file`() {
        val code = """
            import org.koin.dsl.module

            val moduleA = module { }
            val moduleB = module { }
            val moduleC = module { }
        """.trimIndent()

        val findings = EmptyModule(Config.empty).lint(code)
        assertThat(findings).hasSize(3)
    }

    // Edge case: Empty module with named parameters
    @Test
    fun `reports empty module with named createdAtStart parameter`() {
        val code = """
            import org.koin.dsl.module

            val m = module(createdAtStart = true) { }
        """.trimIndent()

        val findings = EmptyModule(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    // Edge case: Empty module as nested expression
    @Test
    fun `reports empty module returned from function`() {
        val code = """
            import org.koin.dsl.module

            fun createModule() = module { }
        """.trimIndent()

        val findings = EmptyModule(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    // Edge case: Empty module in list initialization
    @Test
    fun `reports empty modules in list`() {
        val code = """
            import org.koin.dsl.module

            val allModules = listOf(
                module { },
                module { single { Service() } },
                module { }
            )
        """.trimIndent()

        val findings = EmptyModule(Config.empty).lint(code)
        assertThat(findings).hasSize(2)
    }

    // Edge case: Empty module with trailing lambda syntax
    @Test
    fun `reports empty module with explicit lambda receiver`() {
        val code = """
            import org.koin.dsl.module

            val m = module { }
        """.trimIndent()

        val findings = EmptyModule(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
