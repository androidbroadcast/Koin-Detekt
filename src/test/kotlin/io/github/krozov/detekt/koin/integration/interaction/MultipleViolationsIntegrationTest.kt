package io.github.krozov.detekt.koin.integration.interaction

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Integration tests verifying multiple violations in a single file are correctly detected.
 */
class MultipleViolationsIntegrationTest {

    private val ruleSet = KoinRuleSetProvider().instance(Config.empty)

    @Test
    fun `detects multiple different violations in one file`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get
            import org.koin.core.component.inject
            import org.koin.dsl.module
            import android.app.Activity

            // Violation 1: NoKoinComponentInterface (KoinComponent in non-framework class)
            class MyRepository : KoinComponent {
                // Violation 2: NoGetOutsideModuleDefinition (get outside module)
                val api = get<ApiService>()

                // Violation 3: NoInjectDelegate (inject delegate)
                val cache: Cache by inject()
            }

            // Violation 4: EmptyModule
            val emptyModule = module { }

            // Violation 5: MissingScopeClose (createScope without close)
            class SessionManager : KoinComponent {
                val scope = getKoin().createScope("session")
            }

            // Violation 6: StartKoinInActivity
            class MainActivity : Activity() {
                override fun onCreate() {
                    super.onCreate()
                    startKoin {
                        modules(emptyModule)
                    }
                }
            }

            interface ApiService
            interface Cache
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // Should detect at least 6 violations
        assertThat(findings.size)
            .withFailMessage("Expected at least 6 violations, got: ${findings.map { it.issue.id }}")
            .isGreaterThanOrEqualTo(6)

        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).contains(
            "NoKoinComponentInterface",
            "NoGetOutsideModuleDefinition",
            "NoInjectDelegate",
            "EmptyModule",
            "MissingScopeClose",
            "StartKoinInActivity"
        )
    }

    @Test
    fun `does not duplicate violations for same issue`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyRepository : KoinComponent {
                val api = get<ApiService>()
            }

            class MyRepository2 : KoinComponent {
                val api = get<ApiService>()
            }

            interface ApiService
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // Should have exactly 4 violations:
        // 2 x NoKoinComponentInterface (one per class)
        // 2 x NoGetOutsideModuleDefinition (one per class)
        assertThat(findings.size).isEqualTo(4)

        val koinComponentFindings = findings.filter { it.issue.id == "NoKoinComponentInterface" }
        val getFindings = findings.filter { it.issue.id == "NoGetOutsideModuleDefinition" }

        assertThat(koinComponentFindings).hasSize(2)
        assertThat(getFindings).hasSize(2)
    }

    @Test
    fun `correctly reports violation positions`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyRepository : KoinComponent {
                val api = get<ApiService>()
            }

            class AnotherRepository : KoinComponent {
                val api = get<ApiService>()
            }

            interface ApiService
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // Check that NoGetOutsideModuleDefinition findings are reported with line numbers
        val getFindings = findings.filter { it.issue.id == "NoGetOutsideModuleDefinition" }
        assertThat(getFindings).isNotEmpty()

        // All findings must have valid positive line numbers
        getFindings.forEach { finding ->
            assertThat(finding.location.source.line).isGreaterThan(0)
        }
    }

    @Test
    fun `handles multiple violations in same statement`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyClass : KoinComponent {
                val api = get<ApiService>()  // Violation: NoGetOutsideModuleDefinition
            }

            // Violation: EmptyModule
            val module = module { }

            // Violation: MissingScopeClose
            class ScopeManager : KoinComponent {
                val scope = getKoin().createScope("scope1")
            }

            interface ApiService
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        assertThat(findings.size).isGreaterThanOrEqualTo(3)

        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).contains(
            "NoKoinComponentInterface",
            "NoGetOutsideModuleDefinition",
            "EmptyModule",
            "MissingScopeClose"
        )
    }

    @Test
    fun `detects violations across multiple modules in one file`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            // Violation: DuplicateBindingWithoutQualifier
            val module = module {
                single { ServiceA() } bind InterfaceX::class
                single { ServiceB() } bind InterfaceX::class  // Duplicate binding
            }

            // Violation: GenericDefinitionWithoutQualifier
            val anotherModule = module {
                single { listOf<String>() }  // Generic without qualifier
                single { listOf<Int>() }     // Same type, collision risk
            }

            interface InterfaceX
            class ServiceA : InterfaceX
            class ServiceB : InterfaceX
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).contains(
            "DuplicateBindingWithoutQualifier",
            "GenericDefinitionWithoutQualifier"
        )
    }

    @Test
    fun `detects violations in nested structures`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class OuterClass : KoinComponent {
                // Violation: NoKoinComponentInterface (KoinComponent in non-framework class)
                val api = get<ApiService>()

                inner class InnerClass {
                    // Violation: NoGetOutsideModuleDefinition (get in nested class)
                    fun doSomething() {
                        val service = get<OtherService>()
                    }
                }
            }

            interface ApiService
            interface OtherService
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).contains(
            "NoKoinComponentInterface",
            "NoGetOutsideModuleDefinition"
        )
    }
}
