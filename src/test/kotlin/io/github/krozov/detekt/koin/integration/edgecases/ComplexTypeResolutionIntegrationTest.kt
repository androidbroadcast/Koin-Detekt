package io.github.krozov.detekt.koin.integration.edgecases

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Integration tests verifying correct type resolution for complex types.
 */
class ComplexTypeResolutionIntegrationTest {

    private val ruleSet = KoinRuleSetProvider().instance(Config.empty)

    @Test
    fun `handles nested generics`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            val module = module {
                // Violation: GenericDefinitionWithoutQualifier
                single { listOf<String>() }
                single { listOf<Int>() }
            }
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).contains("GenericDefinitionWithoutQualifier")
    }

    @Test
    fun `handles deeply nested generics`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            val module = module {
                // Violation: GenericDefinitionWithoutQualifier
                single { mapOf<String, List<Int>>() }
                single { mapOf<String, List<Double>>() }
            }
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).contains("GenericDefinitionWithoutQualifier")
    }

    @Test
    fun `handles star projections`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            val module = module {
                // Violation: GenericDefinitionWithoutQualifier
                single { listOf<*>() }
                single { mapOf<String, *>() }
            }
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).contains("GenericDefinitionWithoutQualifier")
    }

    @Test
    fun `handles type aliases`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            typealias UserId = String
            typealias UserList = List<User>

            data class User(val id: UserId)

            val module = module {
                single { UserList() }
            }
        """.trimIndent()

        // Should handle type aliases without crashing
        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }
        assertThat(findings).isNotNull()
    }

    @Test
    fun `handles nested classes`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            class Outer {
                class Inner {
                    class DeepInner
                }
            }

            val module = module {
                single { Outer.Inner.DeepInner() }
            }
        """.trimIndent()

        // Should handle nested classes without crashing
        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }
        assertThat(findings).isNotNull()
    }

    @Test
    fun `handles generic nested classes`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            class Container<T> {
                inner class Item<R>
            }

            val module = module {
                single { Container<String>().Item<Int>() }
            }
        """.trimIndent()

        // Should handle generic nested classes without crashing
        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }
        assertThat(findings).isNotNull()
    }

    @Test
    fun `handles variance annotations`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            interface Producer<out T> {
                fun produce(): T
            }

            interface Consumer<in T> {
                fun consume(item: T)
            }

            class StringProducer : Producer<String> {
                override fun produce() = "Hello"
            }

            val module = module {
                single { StringProducer() }
            }
        """.trimIndent()

        // Should handle variance annotations without crashing
        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }
        assertThat(findings).isNotNull()
    }

    @Test
    fun `handles reified type parameters`() {
        val code = """
            package com.example

            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            inline fun <reified T> KoinComponent.getTyped(): T = get()

            class Service : KoinComponent {
                fun <T> create(): T = getTyped<T>()
            }
        """.trimIndent()

        // Should handle reified type parameters without crashing
        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }
        assertThat(findings).isNotNull()

        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).contains(
            "NoKoinComponentInterface",
            "NoGetOutsideModuleDefinition"
        )
    }

    @Test
    fun `handles generic bounds`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            interface Comparable<T> {
                fun compareTo(other: T): Int
            }

            class NumberComparator : Comparable<Number> {
                override fun compareTo(other: Number): Int = 0
            }

            val module = module {
                single { NumberComparator() }
            }
        """.trimIndent()

        // Should handle generic bounds without crashing
        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }
        assertThat(findings).isNotNull()
    }

    @Test
    fun `detects violations in complex generic scenarios`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            val module = module {
                // Violation: GenericDefinitionWithoutQualifier - collection generics without qualifier
                single { listOf<String>() }
                single { listOf<Int>() }
            }
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).contains("GenericDefinitionWithoutQualifier")
    }
}