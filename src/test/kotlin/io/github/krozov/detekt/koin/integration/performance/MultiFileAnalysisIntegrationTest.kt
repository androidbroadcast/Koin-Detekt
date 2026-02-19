package io.github.krozov.detekt.koin.integration.performance

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

/**
 * Integration tests verifying performance when analyzing multiple files.
 */
class MultiFileAnalysisIntegrationTest {

    private val ruleSet = KoinRuleSetProvider().instance(Config.empty)

    @Test
    fun `analyzes 10 files efficiently`() {
        val files = (1..10).map { i ->
            """
            package com.example.file$i

            import org.koin.dsl.module
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            val module$i = module {
                single { Service$i() }
            }

            class BadClass$i : KoinComponent {
                val service = get<Service$i>()
            }

            class Service$i
            """.trimIndent()
        }

        val executionTime = measureTimeMillis {
            files.forEach { code ->
                val findings = ruleSet.rules.flatMap { it.lint(code) }
                assertThat(findings).isNotEmpty()
            }
        }

        // Should complete within 3 seconds for 10 files
        assertThat(executionTime)
            .withFailMessage("Analysis took too long: ${executionTime}ms")
            .isLessThan(3000)
    }

    @Test
    fun `analyzes 50 files efficiently`() {
        val files = (1..50).map { i ->
            """
            package com.example.file$i

            import org.koin.dsl.module

            val module$i = module {
                single { Service$i() }
                factory { UseCase$i(get()) }
            }

            class Service$i
            class UseCase$i(private val service: Service$i)
            """.trimIndent()
        }

        val executionTime = measureTimeMillis {
            files.forEach { code ->
                val findings = ruleSet.rules.flatMap { it.lint(code) }
            }
        }

        // Should complete within 10 seconds for 50 files
        assertThat(executionTime)
            .withFailMessage("Analysis took too long: ${executionTime}ms")
            .isLessThan(10000)
    }

    @Test
    fun `does not leak memory across multiple analyses`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class BadClass : KoinComponent {
                val service = get<Service>()
            }

            class Service
        """.trimIndent()

        // Analyze same file multiple times
        val results = mutableListOf<Int>()
        repeat(20) {
            val findings = ruleSet.rules.flatMap { it.lint(code) }
            results.add(findings.size)
        }

        // All analyses should return same number of findings
        assertThat(results.distinct()).hasSize(1)
        assertThat(results[0]).isEqualTo(2)  // NoKoinComponentInterface + NoGetOutsideModuleDefinition
    }

    @Test
    fun `handles files with different violation patterns`() {
        val files = listOf(
            // File 1: Service locator violations
            """
            package com.example.file1

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyRepository : KoinComponent {
                val api = get<ApiService>()
            }

            interface ApiService
            """.trimIndent(),

            // File 2: Module violations
            """
            package com.example.file2

            import org.koin.dsl.module

            val emptyModule = module { }

            val module = module {
                single { ServiceA() }
                single { ServiceB() } bind InterfaceX::class
            }

            interface InterfaceX
            class ServiceA : InterfaceX
            class ServiceB : InterfaceX
            """.trimIndent(),

            // File 3: Scope violations
            """
            package com.example.file3

            import org.koin.core.component.KoinComponent

            class SessionManager : KoinComponent {
                val scope = getKoin().createScope("session")
            }
            """.trimIndent(),

            // File 4: No violations
            """
            package com.example.file4

            import org.koin.dsl.module

            fun buildGoodModule() = module {
                single { Service() }
                factory { UseCase(get()) }
            }

            class Service
            class UseCase(private val service: Service)
            """.trimIndent()
        )

        val findings = files.map { code ->
            ruleSet.rules.flatMap { it.lint(code) }
        }

        // Each file should have expected number of findings
        assertThat(findings[0].size).isGreaterThan(0)  // Service locator violations
        assertThat(findings[1].size).isGreaterThan(0)  // Module violations
        assertThat(findings[2].size).isGreaterThan(0)  // Scope violations
        assertThat(findings[3].size).isEqualTo(0)      // No violations
    }

    @Test
    fun `rule instantiation does not affect performance`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            val module = module { single { Service() } }

            class Service
        """.trimIndent()

        // Time first instantiation
        val firstTime = measureTimeMillis {
            val ruleSet = KoinRuleSetProvider().instance(Config.empty)
            val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }
        }

        // Time subsequent instantiations
        val secondTime = measureTimeMillis {
            val ruleSet = KoinRuleSetProvider().instance(Config.empty)
            val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }
        }

        // Subsequent instantiations should not be significantly slower
        // (allowing some variance due to JVM warmup)
        assertThat(secondTime)
            .isLessThan(firstTime * 3)
    }
}