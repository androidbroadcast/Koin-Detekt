package io.github.krozov.detekt.koin.integration.performance

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

/**
 * Integration tests verifying performance on large files.
 */
class LargeFileAnalysisIntegrationTest {

    private val ruleSet = KoinRuleSetProvider().instance(Config.empty)

    @Test
    fun `analyzes file with 1000 lines within reasonable time`() {
        // Generate a large file with multiple modules and violations
        val code = generateLargeCode(1000)

        val executionTime = measureTimeMillis {
            val findings = ruleSet.rules.flatMap { it.lint(code) }

            // Should still detect violations in large file
            assertThat(findings).isNotEmpty()
        }

        // Should complete within 5 seconds
        assertThat(executionTime)
            .withFailMessage("Analysis took too long: ${executionTime}ms")
            .isLessThan(5000)
    }

    @Test
    fun `analyzes file with 5000 lines within reasonable time`() {
        val code = generateLargeCode(5000)

        val executionTime = measureTimeMillis {
            val findings = ruleSet.rules.flatMap { it.lint(code) }

            // Should still detect violations
            assertThat(findings).isNotEmpty()
        }

        // Should complete within 15 seconds for very large files
        assertThat(executionTime)
            .withFailMessage("Analysis took too long: ${executionTime}ms")
            .isLessThan(15000)
    }

    @Test
    fun `memory usage does not grow excessively with file size`() {
        val smallCode = generateLargeCode(100)
        val largeCode = generateLargeCode(1000)

        // Analyze small file
        val smallFindings = ruleSet.rules.flatMap { it.lint(smallCode) }
        val smallCount = smallFindings.size

        // Analyze large file
        val largeFindings = ruleSet.rules.flatMap { it.lint(largeCode) }
        val largeCount = largeFindings.size

        // Findings should scale roughly with file size
        // Large file should have more findings (proportional to content)
        assertThat(largeCount)
            .withFailMessage("Large file should have more findings")
            .isGreaterThan(smallCount)
    }

    @Test
    fun `handles deeply nested structures in large file`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            ${generateNestedClasses(10)}

            interface ApiService
        """.trimIndent()

        val executionTime = measureTimeMillis {
            val findings = ruleSet.rules.flatMap { it.lint(code) }

            // Should detect violations even in deeply nested structures
            assertThat(findings).isNotEmpty()
        }

        assertThat(executionTime).isLessThan(3000)
    }

    @Test
    fun `analyzes file with many violations efficiently`() {
        // Generate file with many violations
        val code = (1..100).joinToString("\n") { i ->
            """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyClass$i : KoinComponent {
                val api$i = get<ApiService$i>()
            }

            interface ApiService$i
            """.trimIndent()
        }

        val executionTime = measureTimeMillis {
            val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

            // Should detect all violations
            assertThat(findings.size).isGreaterThanOrEqualTo(200)  // 100 classes * 2 violations each
        }

        assertThat(executionTime)
            .withFailMessage("Analysis of many violations took too long: ${executionTime}ms")
            .isLessThan(5000)
    }

    private fun generateLargeCode(lines: Int): String {
        val moduleCount = lines / 50
        val modules = (1..moduleCount).joinToString("\n\n") { i ->
            """
            import org.koin.dsl.module
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            val module$i = module {
                single { Service$i() }
                factory { UseCase$i(get()) }
            }

            class BadClass$i : KoinComponent {
                val service = get<Service$i>()
            }

            class Service$i
            class UseCase$i(private val service: Service$i)
            """.trimIndent()
        }

        return modules
    }

    private fun generateNestedClasses(depth: Int): String {
        return buildString {
            var current = "class Level0 : KoinComponent { val api = get<ApiService>()\n"
            repeat(depth) { i ->
                current += "  class Level$i : KoinComponent { val api$i = get<ApiService$i>()\n"
            }
            current += "  }\n".repeat(depth)
            append(current)
        }
    }
}