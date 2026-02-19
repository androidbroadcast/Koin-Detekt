package io.github.krozov.detekt.koin.integration.interaction

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Integration tests verifying rule priority and conflict resolution.
 */
class RulePriorityIntegrationTest {

    private val ruleSet = KoinRuleSetProvider().instance(Config.empty)

    @Test
    fun `specific rules take precedence over general rules`() {
        val code = """
            package com.example

            import androidx.fragment.app.Fragment
            import org.koin.androidx.scope.activityScope

            class MyFragment : Fragment() {
                // Should trigger ActivityFragmentKoinScope (specific)
                // Not general NoKoinComponentInterface
                private val vm by activityScope().get<MyViewModel>()
            }

            class MyViewModel
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val ruleIds = findings.map { it.issue.id }

        // ActivityFragmentKoinScope is more specific and should be reported
        assertThat(ruleIds).contains("ActivityFragmentKoinScope")
    }

    @Test
    fun `multiple rules can report on same code section`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyRepository : KoinComponent {
                // Should trigger both:
                // 1. NoKoinComponentInterface (class implements KoinComponent)
                // 2. NoGetOutsideModuleDefinition (get() outside module)
                val api = get<ApiService>()
            }

            interface ApiService
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val ruleIds = findings.map { it.issue.id }

        // Both rules should report on different aspects
        assertThat(ruleIds).containsExactlyInAnyOrder(
            "NoKoinComponentInterface",
            "NoGetOutsideModuleDefinition"
        )
    }

    @Test
    fun `annotation rules and DSL rules coexist`() {
        val code = """
            package com.example

            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Single
            import org.koin.dsl.module

            // Should trigger MixingDslAndAnnotations
            @Module
            class AnnotatedModule {
                @Single
                fun provideRepo(): Repository = RepositoryImpl()
            }

            val dslModule = module {
                single { ApiService() }
            }

            interface Repository
            class RepositoryImpl : Repository
            class ApiService
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).contains("MixingDslAndAnnotations")
    }

    @Test
    fun `platform rules only apply to relevant code`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get
            import org.koin.dsl.module

            // General violation - should be reported
            class MyService : KoinComponent {
                val repo = get<Repository>()
            }

            // Compose violation - should be reported
            import androidx.compose.runtime.Composable
            import org.koin.androidx.compose.koinViewModel

            fun MyScreen() {
                val vm = koinViewModel<MyViewModel>()
            }

            class MyViewModel

            interface Repository
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val ruleIds = findings.map { it.issue.id }

        // Both general and platform rules should work
        assertThat(ruleIds).contains(
            "NoKoinComponentInterface",
            "NoGetOutsideModuleDefinition",
            "KoinViewModelOutsideComposable"
        )
    }

    @Test
    fun `severity levels are correctly assigned`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.dsl.module

            class MyService : KoinComponent {
                val repo = get<Repository>()
            }

            val module = module { }

            interface Repository
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // All findings should have severity set (not null)
        findings.forEach { finding ->
            assertThat(finding.issue.severity).isNotNull()
        }
    }

    @Test
    fun `deprecated API rule triggers alongside other rules`() {
        val code = """
            package com.example

            import org.koin.dsl.module
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            object AppModules {
                val module = module {
                    single { Service() }
                }
            }

            // Should trigger DeprecatedKoinApi
            fun runCheck() {
                checkModules { modules(AppModules.module) }
            }

            // Also triggers other rules
            class BadClass : KoinComponent {
                val service = get<Service>()
            }

            class Service
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val ruleIds = findings.map { it.issue.id }

        assertThat(ruleIds).contains(
            "DeprecatedKoinApi",
            "NoKoinComponentInterface",
            "NoGetOutsideModuleDefinition"
        )
    }

    @Test
    fun `architecture rules require configuration to be active`() {
        // By default, LayerBoundaryViolation is inactive
        val code = """
            package com.example.domain

            import org.koin.core.component.get

            class UseCase {
                val repo = get<Repository>()
            }

            interface Repository
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val layerViolation = findings.filter { it.issue.id == "LayerBoundaryViolation" }

        // Should not report by default (inactive rule)
        assertThat(layerViolation).isEmpty()
    }
}
