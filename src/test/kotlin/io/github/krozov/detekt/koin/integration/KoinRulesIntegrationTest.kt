package io.github.krozov.detekt.koin.integration

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.ServiceLoader

/**
 * Integration tests verifying Detekt plugin loading and real-world usage.
 *
 * Tests that KoinRuleSetProvider works correctly within Detekt's runtime
 * environment, beyond isolated unit tests.
 */
class KoinRulesIntegrationTest {
    @Test
    fun `ServiceLoader can discover KoinRuleSetProvider`() {
        val providers = ServiceLoader.load(RuleSetProvider::class.java).toList()
        val koinProvider = providers.find { it is KoinRuleSetProvider }

        assertThat(koinProvider)
            .withFailMessage("KoinRuleSetProvider not discovered via ServiceLoader")
            .isNotNull()

        val ruleSet = koinProvider!!.instance(Config.empty)

        assertThat(ruleSet.id).isEqualTo("koin-rules")
        assertThat(ruleSet.rules).hasSize(14)

        // Verify all rule names
        val ruleIds = ruleSet.rules.map { it.ruleId }
        assertThat(ruleIds).containsExactlyInAnyOrder(
            "NoGetOutsideModuleDefinition",
            "NoInjectDelegate",
            "NoKoinComponentInterface",
            "NoGlobalContextAccess",
            "NoKoinGetInApplication",
            "EmptyModule",
            "SingleForNonSharedDependency",
            "MissingScopedDependencyQualifier",
            "DeprecatedKoinApi",
            "ModuleIncludesOrganization",
            "MissingScopeClose",
            "ScopedDependencyOutsideScopeBlock",
            "FactoryInScopeBlock",
            "KtorRequestScopeMisuse"
        )
    }

    @Test
    fun `E2E analysis detects multiple violations in real code`() {
        val code = """
            package test

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyRepository : KoinComponent {
                val apiService = get<ApiService>()
            }

            interface ApiService
        """.trimIndent()

        val provider = KoinRuleSetProvider()
        val ruleSet = provider.instance(Config.empty)

        // Use Detekt test API to lint code
        val findings = ruleSet.rules.flatMap { rule ->
            rule.lint(code)
        }

        // Should find 2 violations:
        // 1. NoKoinComponentInterface (MyRepository implements KoinComponent)
        // 2. NoGetOutsideModuleDefinition (get() outside module)
        assertThat(findings).hasSize(2)

        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).containsExactlyInAnyOrder(
            "NoKoinComponentInterface",
            "NoGetOutsideModuleDefinition"
        )
    }
}
