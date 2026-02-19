package io.github.krozov.detekt.koin.integration.interaction

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Integration tests verifying suppression annotations work correctly.
 */
class SuppressionIntegrationTest {

    private val ruleSet = KoinRuleSetProvider().instance(Config.empty)

    @Test
    fun `suppress annotation at class level prevents violations`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get
            import org.koin.core.component.inject

            @Suppress("NoKoinComponentInterface", "NoGetOutsideModuleDefinition", "NoInjectDelegate")
            class MyRepository : KoinComponent {
                val api = get<ApiService>()
                val cache: Cache by inject()
            }

            interface ApiService
            interface Cache
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // All violations should be suppressed
        assertThat(findings).isEmpty()
    }

    @Test
    fun `suppress annotation at file level prevents violations`() {
        val code = """
            @file:Suppress("NoKoinComponentInterface", "NoGetOutsideModuleDefinition")

            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyRepository : KoinComponent {
                val api = get<ApiService>()
            }

            interface ApiService
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // All violations should be suppressed
        assertThat(findings).isEmpty()
    }

    @Test
    fun `suppress annotation at statement level prevents violation`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyRepository : KoinComponent {
                val api = @Suppress("NoGetOutsideModuleDefinition") {
                    get<ApiService>()
                }
            }

            interface ApiService
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // NoGetOutsideModuleDefinition should be suppressed, but NoKoinComponentInterface should still be reported
        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).containsOnly("NoKoinComponentInterface")
    }

    @Test
    fun `partial suppression allows other violations to be reported`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get
            import org.koin.core.component.inject

            @Suppress("NoKoinComponentInterface")
            class MyRepository : KoinComponent {
                val api = get<ApiService>()  // Not suppressed
                val cache: Cache by inject()  // Not suppressed
            }

            interface ApiService
            interface Cache
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // NoKoinComponentInterface suppressed, but other violations should be reported
        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).containsExactlyInAnyOrder(
            "NoGetOutsideModuleDefinition",
            "NoInjectDelegate"
        )
        assertThat(ruleIds).doesNotContain("NoKoinComponentInterface")
    }

    @Test
    fun `suppress by exact rule IDs suppresses all specified rules`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            @Suppress("NoKoinComponentInterface", "NoGetOutsideModuleDefinition")
            class MyRepository : KoinComponent {
                val api = get<ApiService>()
            }

            interface ApiService
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // All specified Koin rules should be suppressed
        assertThat(findings).isEmpty()
    }

    @Test
    fun `suppress multiple rules by listing exact IDs`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            @Suppress("NoKoinComponentInterface", "NoGetOutsideModuleDefinition")
            class MyRepository : KoinComponent {
                val api = get<ApiService>()
            }

            interface ApiService
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // All suppressed rules should not be reported
        assertThat(findings).isEmpty()
    }

    @Test
    fun `suppress only specific violations in complex file`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get
            import org.koin.dsl.module

            @Suppress("NoKoinComponentInterface")
            class MyRepository : KoinComponent {
                val api = get<ApiService>()  // Should report NoGetOutsideModuleDefinition
            }

            @Suppress("EmptyModule", "ModuleAsTopLevelVal")
            val emptyModule = module { }  // Should NOT report

            class AnotherRepository : KoinComponent {  // Should report NoKoinComponentInterface
                val api = get<ApiService>()  // Should report NoGetOutsideModuleDefinition
            }

            interface ApiService
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val ruleIds = findings.map { it.issue.id }

        // EmptyModule and ModuleAsTopLevelVal are suppressed
        assertThat(ruleIds).doesNotContain("EmptyModule")
        assertThat(ruleIds).doesNotContain("ModuleAsTopLevelVal")
        // Both NoGetOutsideModuleDefinition and NoKoinComponentInterface are reported
        assertThat(ruleIds).contains("NoGetOutsideModuleDefinition", "NoKoinComponentInterface")
    }

    @Test
    fun `suppress annotation on property`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyRepository : KoinComponent {
                @Suppress("NoGetOutsideModuleDefinition")
                val api = get<ApiService>()

                val other = get<OtherService>()  // Should report
            }

            interface ApiService
            interface OtherService
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val getFindings = findings.filter { it.issue.id == "NoGetOutsideModuleDefinition" }
        assertThat(getFindings).hasSize(1)  // Only `other` should be reported
    }
}
