package io.github.krozov.detekt.koin.integration.compatibility

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Integration tests verifying compatibility with modern Kotlin features.
 */
class KotlinVersionCompatibilityTest {

    private val ruleSet = KoinRuleSetProvider().instance(Config.empty)

    @Test
    fun `handles context receivers`() {
        // Note: This uses -Xcontext-receivers compiler flag
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            context(KoinComponent)
            class MyClass {
                fun doSomething() {
                    val service = get<Service>()
                }
            }

            interface Service
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // Should detect violations even with context receivers
        val getFindings = findings.filter { it.issue.id == "NoGetOutsideModuleDefinition" }
        assertThat(getFindings).isNotEmpty()
    }

    @Test
    fun `handles inline classes`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            @JvmInline
            value class UserId(val value: String)

            object AppModules {
                val module = module {
                    single { UserRepository(get()) }
                }
            }

            class UserRepository(private val userId: UserId)
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // Should not crash on inline classes
        // Should analyze normally
        assertThat(findings).isEmpty()  // No violations in this code
    }

    @Test
    fun `handles value classes`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            value class Username(val name: String)

            object AppModules {
                val module = module {
                    single { UserService(get()) }
                }
            }

            class UserService(private val username: Username)
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // Should not crash on value classes
        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles sealed interfaces`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            sealed interface Result<out T> {
                data class Success<T>(val value: T) : Result<T>
                data class Error(val message: String) : Result<Nothing>
            }

            object AppModules {
                val module = module {
                    single { ResultHandler() }
                }
            }

            class ResultHandler
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // Should not crash on sealed interfaces
        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles generic types with variance`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            interface Producer<out T> {
                fun produce(): T
            }

            interface Consumer<in T> {
                fun consume(item: T)
            }

            object AppModules {
                val module = module {
                    single { StringProducer() }
                    single { StringConsumer() }
                }
            }

            class StringProducer : Producer<String> {
                override fun produce() = "test"
            }

            class StringConsumer : Consumer<String> {
                override fun consume(item: String) {}
            }
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // Should handle variance annotations without crashing
        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles data objects`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            data object SingletonConfig {
                val version = "1.0"
            }

            object AppModules {
                val module = module {
                    single { SingletonConfig }
                }
            }
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // Should handle data objects without crashing
        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles when expressions with subjects`() {
        val code = """
            package com.example

            import org.koin.dsl.module
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class ServiceLocator : KoinComponent {
                fun getService(type: ServiceType): Service {
                    val service = when (type) {
                        ServiceType.API -> get<ApiService>()
                        ServiceType.CACHE -> get<CacheService>()
                    }
                    return service
                }
            }

            enum class ServiceType { API, CACHE }

            interface Service
            interface ApiService : Service
            interface CacheService : Service
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // Should detect violations in when expressions
        val getFindings = findings.filter { it.issue.id == "NoGetOutsideModuleDefinition" }
        assertThat(getFindings).hasSize(2)  // Two get() calls
    }

    @Test
    fun `handles trailing commas`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            object AppModules {
                val module = module {
                    single {
                        ApiService(
                            baseUrl = "https://api.example.com",
                            timeout = 30_000,
                            retries = 3,
                        )
                    }
                }
            }

            class ApiService(
                val baseUrl: String,
                val timeout: Int,
                val retries: Int,
            )
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // Should handle trailing commas without crashing
        assertThat(findings).isEmpty()
    }
}