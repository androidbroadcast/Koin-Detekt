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

    @Test
    fun `custom configuration is applied to rules`() {
        // Test that custom config for a rule is properly applied
        // when instantiated through the RuleSetProvider.
        // Pass config directly to the rule since RuleSetProvider passes the same
        // config to all rules and each rule uses subConfig to get its specific config.
        val ruleSpecificConfig = TestConfig(
            "allowedSuperTypes" to listOf("Application", "Activity", "CustomFramework")
        )

        // Create a wrapper config that provides the rule-specific config as a sub-config
        val config = object : Config {
            override fun subConfig(key: String): Config {
                return if (key == "NoKoinComponentInterface") ruleSpecificConfig else Config.empty
            }

            override fun <T : Any> valueOrDefault(key: String, default: T): T {
                @Suppress("UNCHECKED_CAST")
                return default
            }

            override fun <T : Any> valueOrNull(key: String): T? = null
        }

        val provider = KoinRuleSetProvider()
        val ruleSet = provider.instance(config)

        val code = """
            import org.koin.core.component.KoinComponent

            class MyApp : CustomFramework(), KoinComponent
        """.trimIndent()

        val rule = ruleSet.rules.find { it.ruleId == "NoKoinComponentInterface" }
        assertThat(rule).isNotNull()

        val findings = rule!!.lint(code)

        // Should NOT report violation because CustomFramework is in allowedSuperTypes
        assertThat(findings).isEmpty()
    }
}
