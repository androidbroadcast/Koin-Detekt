package io.github.krozov.detekt.koin.integration.configuration

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Integration tests verifying YAML configuration is correctly applied to rules.
 */
class YamlConfigurationIntegrationTest {

    @Test
    fun `inactive rule does not report violations`() {
        // Create config where ModuleIncludesOrganization is inactive
        val config = object : Config {
            override fun subConfig(key: String): Config {
                return if (key == "ModuleIncludesOrganization") {
                    object : Config {
                        override fun <T : Any> valueOrDefault(key: String, default: T): T {
                            @Suppress("UNCHECKED_CAST")
                            return if (key == "active") false as T else default
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
        val rule = ruleSet.rules.find { it.ruleId == "ModuleIncludesOrganization" }

        assertThat(rule).isNotNull()

        val code = """
            import org.koin.dsl.module

            val module1 = module { single { ServiceA() } }
            val module2 = module { single { ServiceB() } }

            // Should trigger ModuleIncludesOrganization if active
            val bigModule = module {
                includes(module1, module2)
                single { ServiceC() }
                single { ServiceD() }
                single { ServiceE() }
            }

            class ServiceA
            class ServiceB
            class ServiceC
            class ServiceD
            class ServiceE
        """.trimIndent()

        val findings = rule!!.lint(code)

        // Rule is inactive, should not report violations
        assertThat(findings).isEmpty()
    }

    @Test
    fun `custom allowedSuperTypes configuration is applied`() {
        // Create config with custom allowedSuperTypes
        val config = object : Config {
            override fun subConfig(key: String): Config {
                return if (key == "NoKoinComponentInterface") {
                    object : Config {
                        override fun <T : Any> valueOrDefault(key: String, default: T): T {
                            @Suppress("UNCHECKED_CAST")
                            return if (key == "allowedSuperTypes") {
                                listOf("Application", "Activity", "CustomFramework") as T
                            } else {
                                default
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

            // Should NOT report because CustomFramework is allowed
            class MyApp : CustomFramework(), KoinComponent

            class CustomFramework
        """.trimIndent()

        val findings = rule!!.lint(code)

        // Should not report violation
        assertThat(findings).isEmpty()
    }

    @Test
    fun `custom namePatterns configuration is applied`() {
        // Create config with custom namePatterns
        val config = object : Config {
            override fun subConfig(key: String): Config {
                return if (key == "SingleForNonSharedDependency") {
                    object : Config {
                        override fun <T : Any> valueOrDefault(key: String, default: T): T {
                            @Suppress("UNCHECKED_CAST")
                            return when (key) {
                                "active" -> true as T
                                "namePatterns" -> listOf(".*UseCase", ".*Command", ".*Helper") as T
                                else -> default
                            }
                        }
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : Any> valueOrNull(key: String): T? =
                            if (key == "active") true as T else null
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
        val rule = ruleSet.rules.find { it.ruleId == "SingleForNonSharedDependency" }

        assertThat(rule).isNotNull()

        val code = """
            import org.koin.dsl.module

            val module = module {
                // Should report - Helper matches pattern
                single { UserHelper(get()) }
            }

            interface Api
            class UserHelper(private val api: Api)
        """.trimIndent()

        val findings = rule!!.lint(code)

        // Should report violation for UserHelper
        assertThat(findings).isNotEmpty()
    }

    @Test
    fun `LayerBoundaryViolation with custom restrictedLayers configuration`() {
        // Create config with LayerBoundaryViolation active
        val config = object : Config {
            override fun subConfig(key: String): Config {
                return if (key == "LayerBoundaryViolation") {
                    object : Config {
                        override fun <T : Any> valueOrDefault(key: String, default: T): T {
                            @Suppress("UNCHECKED_CAST")
                            return when (key) {
                                "active" -> true as T
                                "restrictedLayers" -> listOf("com.example.domain") as T
                                "allowedImports" -> emptyList<String>() as T
                                else -> default
                            }
                        }
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : Any> valueOrNull(key: String): T? = when (key) {
                            "restrictedLayers" -> listOf("com.example.domain") as T
                            "allowedImports" -> emptyList<String>() as T
                            else -> null
                        }
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
        val rule = ruleSet.rules.find { it.ruleId == "LayerBoundaryViolation" }

        assertThat(rule).isNotNull()

        val code = """
            package com.example.domain

            import org.koin.core.component.get

            class UseCase {
                // Should report - Koin import in domain layer
                val repo = get<Repository>()
            }

            interface Repository
        """.trimIndent()

        val findings = rule!!.lint(code)

        // Should report violation
        assertThat(findings).isNotEmpty()
    }

    @Test
    fun `PlatformImportRestriction with custom restrictions configuration`() {
        // Create config with PlatformImportRestriction active
        val config = object : Config {
            override fun subConfig(key: String): Config {
                return if (key == "PlatformImportRestriction") {
                    object : Config {
                        private val restrictionsList = listOf(
                            mapOf(
                                "import" to "org.koin.android.*",
                                "allowedPackages" to listOf("com.example.android")
                            )
                        )
                        override fun <T : Any> valueOrDefault(key: String, default: T): T {
                            @Suppress("UNCHECKED_CAST")
                            return when (key) {
                                "active" -> true as T
                                "restrictions" -> restrictionsList as T
                                else -> default
                            }
                        }
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : Any> valueOrNull(key: String): T? = when (key) {
                            "restrictions" -> restrictionsList as T
                            else -> null
                        }
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
        val rule = ruleSet.rules.find { it.ruleId == "PlatformImportRestriction" }

        assertThat(rule).isNotNull()

        val code = """
            package com.example.shared

            import org.koin.android.ext.koin.androidContext

            // Should report - Android import in shared module
            class SharedService
        """.trimIndent()

        val findings = rule!!.lint(code)

        // Should report violation
        assertThat(findings).isNotEmpty()
    }

    @Test
    fun `maxInjectedParams configuration is applied`() {
        // Create config with custom maxInjectedParams
        val config = object : Config {
            override fun subConfig(key: String): Config {
                return if (key == "TooManyInjectedParams") {
                    object : Config {
                        override fun <T : Any> valueOrDefault(key: String, default: T): T {
                            @Suppress("UNCHECKED_CAST")
                            return when (key) {
                                "active" -> true as T
                                "maxInjectedParams" -> 3 as T
                                else -> default
                            }
                        }
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : Any> valueOrNull(key: String): T? =
                            if (key == "active") true as T else null
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
                @InjectedParam val d: Float  // 4th - should report with max=3
            )
        """.trimIndent()

        val findings = rule!!.lint(code)

        // Should report violation for 4th parameter
        assertThat(findings).isNotEmpty()
    }
}
