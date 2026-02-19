package io.github.krozov.detekt.koin.integration.configuration

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Integration tests verifying rule configuration overrides work correctly.
 */
class RuleConfigurationOverrideTest {

    @Test
    fun `unknown configuration parameters are ignored without error`() {
        // Config with unknown parameter - should be ignored
        val config = object : Config {
            override fun subConfig(key: String): Config {
                return if (key == "NoKoinComponentInterface") {
                    object : Config {
                        override fun <T : Any> valueOrDefault(key: String, default: T): T {
                            @Suppress("UNCHECKED_CAST")
                            return when (key) {
                                "unknownParameter" -> "someValue" as T  // Unknown - should be ignored
                                "allowedSuperTypes" -> listOf("Application") as T
                                else -> default
                            }
                        }
                        override fun <T : Any> valueOrNull(key: String): T? = null
                        override fun subConfig(key: String): Config = Config.empty
                    }
                } else {
                    Config.empty
                }
            }

            override fun <T : Any> valueOrDefault(key: String, default: T): T = default
            override fun <T : Any> valueOrNull(key: String): T? = null
        }

        val ruleSet = KoinRuleSetProvider().instance(config)
        val rule = ruleSet.rules.find { it.ruleId == "NoKoinComponentInterface" }

        assertThat(rule).isNotNull()

        val code = """
            import org.koin.core.component.KoinComponent
            import android.app.Application

            class MyApp : Application(), KoinComponent
        """.trimIndent()

        // Should not throw exception, should work normally
        val findings = rule!!.lint(code)

        // Application is in allowedSuperTypes, so no violation
        assertThat(findings).isEmpty()
    }

    @Test
    fun `empty configuration uses default values`() {
        val config = Config.empty
        val ruleSet = KoinRuleSetProvider().instance(config)
        val rule = ruleSet.rules.find { it.ruleId == "TooManyInjectedParams" }

        assertThat(rule).isNotNull()

        val code = """
            import org.koin.core.annotation.Single
            import org.koin.core.annotation.InjectedParam

            @Single
            class MyService(
                @InjectedParam val a: String,
                @InjectedParam val b: Int,
                @InjectedParam val c: Long,
                @InjectedParam val d: Float,
                @InjectedParam val e: Double,
                @InjectedParam val f: Boolean  // 6th - default max is 5
            )
        """.trimIndent()

        val findings = rule!!.lint(code)

        // Should report violation with default max=5
        assertThat(findings).isNotEmpty()
    }

    @Test
    fun `multiple rules can have different configurations`() {
        val config = object : Config {
            override fun subConfig(key: String): Config {
                return when (key) {
                    "NoKoinComponentInterface" -> object : Config {
                        override fun <T : Any> valueOrDefault(key: String, default: T): T {
                            @Suppress("UNCHECKED_CAST")
                            return when (key) {
                                "active" -> true as T
                                "allowedSuperTypes" -> listOf("Application", "CustomClass") as T
                                else -> default
                            }
                        }
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : Any> valueOrNull(key: String): T? =
                            if (key == "active") true as T else null
                        override fun subConfig(key: String): Config = Config.empty
                    }
                    "SingleForNonSharedDependency" -> object : Config {
                        override fun <T : Any> valueOrDefault(key: String, default: T): T {
                            @Suppress("UNCHECKED_CAST")
                            return when (key) {
                                "active" -> true as T
                                "namePatterns" -> listOf(".*Service") as T
                                else -> default
                            }
                        }
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : Any> valueOrNull(key: String): T? =
                            if (key == "active") true as T else null
                        override fun subConfig(key: String): Config = Config.empty
                    }
                    else -> Config.empty
                }
            }

            override fun <T : Any> valueOrDefault(key: String, default: T): T = default
            override fun <T : Any> valueOrNull(key: String): T? = null
        }

        val ruleSet = KoinRuleSetProvider().instance(config)

        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.dsl.module

            // NoKoinComponentInterface: CustomClass is allowed
            class MyClass : CustomClass(), KoinComponent

            class CustomClass

            object AppModules {
                val module = module {
                    // SingleForNonSharedDependency: UserService matches pattern
                    single { UserService(get()) }
                }
            }

            interface Api
            class UserService(private val api: Api)
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // Should only report SingleForNonSharedDependency violation
        assertThat(findings).hasSize(1)
        assertThat(findings[0].issue.id).isEqualTo("SingleForNonSharedDependency")
    }

    @Test
    fun `config can disable all rules in rule set`() {
        // Create config that disables all rules
        val config = object : Config {
            override fun subConfig(key: String): Config {
                // Return inactive config for all rules
                return object : Config {
                    override fun <T : Any> valueOrDefault(key: String, default: T): T {
                        @Suppress("UNCHECKED_CAST")
                        return if (key == "active") false as T else default
                    }
                    override fun <T : Any> valueOrNull(key: String): T? = null
                    override fun subConfig(key: String): Config = Config.empty
                }
            }

            override fun <T : Any> valueOrDefault(key: String, default: T): T = default
            override fun <T : Any> valueOrNull(key: String): T? = null
        }

        val ruleSet = KoinRuleSetProvider().instance(config)

        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get
            import org.koin.dsl.module

            // Multiple violations that should all be suppressed
            class BadClass : KoinComponent {
                val api = get<Api>()
            }

            val module = module { }

            interface Api
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // No violations should be reported
        assertThat(findings).isEmpty()
    }

    @Test
    fun `skipCheck configuration for AnnotationProcessorNotConfigured`() {
        val config = object : Config {
            override fun subConfig(key: String): Config {
                return if (key == "AnnotationProcessorNotConfigured") {
                    object : Config {
                        override fun <T : Any> valueOrDefault(key: String, default: T): T {
                            @Suppress("UNCHECKED_CAST")
                            return if (key == "skipCheck") true as T else default
                        }
                        override fun <T : Any> valueOrNull(key: String): T? = null
                        override fun subConfig(key: String): Config = Config.empty
                    }
                } else {
                    Config.empty
                }
            }

            override fun <T : Any> valueOrDefault(key: String, default: T): T = default
            override fun <T : Any> valueOrNull(key: String): T? = null
        }

        val ruleSet = KoinRuleSetProvider().instance(config)
        val rule = ruleSet.rules.find { it.ruleId == "AnnotationProcessorNotConfigured" }

        assertThat(rule).isNotNull()

        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService
        """.trimIndent()

        val findings = rule!!.lint(code)

        // Should not report when skipCheck is true
        assertThat(findings).isEmpty()
    }
}
