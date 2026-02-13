package io.github.krozov.detekt.koin.architecture

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CircularModuleDependencyTest {

    @Test
    fun `reports circular dependency between modules`() {
        val code = """
            val moduleA = module {
                includes(moduleB)
            }

            val moduleB = module {
                includes(moduleA)
            }
        """.trimIndent()

        val findings = CircularModuleDependency(Config.empty).lint(code)

        assertThat(findings).hasSize(2) // Both modules reported
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `allows hierarchical module dependencies`() {
        val code = """
            val coreModule = module {
                single { CoreService() }
            }

            val featureModule = module {
                includes(coreModule)
            }
        """.trimIndent()

        val findings = CircularModuleDependency(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports self-referencing module`() {
        val code = """
            val module = module {
                includes(module)
            }
        """.trimIndent()

        val findings = CircularModuleDependency(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `allows module without includes`() {
        val code = """
            val module = module {
                single { Service() }
            }
        """.trimIndent()

        val findings = CircularModuleDependency(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows module with multiple non-circular includes`() {
        val code = """
            val moduleA = module {
                single { ServiceA() }
            }

            val moduleB = module {
                single { ServiceB() }
            }

            val moduleC = module {
                includes(moduleA, moduleB)
            }
        """.trimIndent()

        val findings = CircularModuleDependency(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report when module is not a Koin module`() {
        val code = """
            val someVar = notAModule {
                includes(otherVar)
            }
        """.trimIndent()

        val findings = CircularModuleDependency(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports both modules in circular dependency`() {
        val code = """
            val moduleA = module {
                includes(moduleB)
            }

            val moduleB = module {
                includes(moduleA)
            }
        """.trimIndent()

        val findings = CircularModuleDependency(Config.empty).lint(code)
        assertThat(findings).hasSize(2)
        assertThat(findings.map { it.message }).allMatch { it.contains("→") }
        assertThat(findings.map { it.message }).allMatch { it.contains("✗ Bad") }
        assertThat(findings.map { it.message }).allMatch { it.contains("✓ Good") }
    }
}
