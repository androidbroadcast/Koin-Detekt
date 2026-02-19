package io.github.krozov.detekt.koin.integration.compatibility

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Integration tests verifying compatibility with different Koin versions.
 */
class KoinVersionCompatibilityTest {

    private val ruleSet = KoinRuleSetProvider().instance(Config.empty)

    @Test
    fun `detects deprecated Koin 3x APIs`() {
        val code = """
            package com.example

            import org.koin.dsl.module
            import io.koin.core.module.Module

            object AppModules {
                val module = module {
                    single { Service() }
                }
            }

            // Deprecated in Koin 4.x
            fun runChecks() {
                checkModules { modules(AppModules.module) }
            }

            class Service
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val deprecatedViolations = findings.filter { it.issue.id == "DeprecatedKoinApi" }
        assertThat(deprecatedViolations)
            .withFailMessage("Should detect deprecated checkModules API")
            .isNotEmpty()
    }

    @Test
    fun `allows modern Koin 4x APIs`() {
        val code = """
            package com.example

            import org.koin.dsl.module
            import org.koin.test.verify.verify
            import io.koin.core.module.Module

            val module = module {
                single { Service() }
            }

            // Modern Koin 4.x API
            verify {
                modules(module)
            }

            class Service
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // Should not report deprecated API violation
        val deprecatedViolations = findings.filter { it.issue.id == "DeprecatedKoinApi" }
        assertThat(deprecatedViolations).isEmpty()
    }

    @Test
    fun `detects koinNavViewModel deprecated API`() {
        val code = """
            package com.example

            import androidx.compose.runtime.Composable
            import org.koin.androidx.compose.koinNavViewModel

            @Composable
            fun MyScreen() {
                // Deprecated API
                val vm = koinNavViewModel<MyViewModel>()
            }

            class MyViewModel
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val deprecatedViolations = findings.filter { it.issue.id == "DeprecatedKoinApi" }
        assertThat(deprecatedViolations)
            .withFailMessage("Should detect deprecated koinNavViewModel API")
            .isNotEmpty()
    }

    @Test
    fun `detects stateViewModel deprecated API`() {
        val code = """
            package com.example

            import androidx.compose.runtime.Composable
            import org.koin.androidx.compose.stateViewModel

            @Composable
            fun MyScreen() {
                // Deprecated API
                val vm = stateViewModel<MyViewModel>()
            }

            class MyViewModel
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val deprecatedViolations = findings.filter { it.issue.id == "DeprecatedKoinApi" }
        assertThat(deprecatedViolations)
            .withFailMessage("Should detect deprecated stateViewModel API")
            .isNotEmpty()
    }

    @Test
    fun `allows modern koinViewModel API`() {
        val code = """
            package com.example

            import androidx.compose.runtime.Composable
            import org.koin.androidx.compose.koinViewModel

            @Composable
            fun MyScreen() {
                // Modern API
                val vm = koinViewModel<MyViewModel>()
            }

            class MyViewModel
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val deprecatedViolations = findings.filter { it.issue.id == "DeprecatedKoinApi" }
        assertThat(deprecatedViolations).isEmpty()
    }

    @Test
    fun `detects deprecated getViewModel API`() {
        val code = """
            package com.example

            import androidx.fragment.app.Fragment
            import org.koin.androidx.viewmodel.ext.android.getViewModel

            class MyFragment : Fragment() {
                // Deprecated API
                val vm = getViewModel<MyViewModel>()
            }

            class MyViewModel
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val deprecatedViolations = findings.filter { it.issue.id == "DeprecatedKoinApi" }
        assertThat(deprecatedViolations)
            .withFailMessage("Should detect deprecated getViewModel API")
            .isNotEmpty()
    }

    @Test
    fun `handles mixed deprecated and modern APIs`() {
        val code = """
            package com.example

            import org.koin.dsl.module
            import org.koin.test.verify.verify

            object AppModules {
                val module = module {
                    single { Service() }
                }
            }

            fun runChecks() {
                // Deprecated
                checkModules { modules(AppModules.module) }

                // Modern
                verify { modules(AppModules.module) }
            }

            class Service
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val deprecatedViolations = findings.filter { it.issue.id == "DeprecatedKoinApi" }

        // Should report only deprecated API
        assertThat(deprecatedViolations).hasSize(1)
    }
}