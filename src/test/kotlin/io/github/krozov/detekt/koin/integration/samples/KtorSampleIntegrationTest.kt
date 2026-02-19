package io.github.krozov.detekt.koin.integration.samples

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Integration tests verifying Detekt rules work correctly on Ktor sample code.
 */
class KtorSampleIntegrationTest {

    private val ruleSet = KoinRuleSetProvider().instance(Config.empty)

    @Test
    fun `detects KtorApplicationKoinInit violations`() {
        val code = """
            package com.example.app

            import io.ktor.server.application.*
            import io.ktor.server.routing.*
            import org.koin.ktor.plugin.Koin
            import org.koin.dsl.module

            fun Application.module() {
                routing {
                    // BAD: install(Koin) in routing block
                    install(Koin) {
                        modules(appModule)
                    }
                    get("/api") { }
                }
            }

            val appModule = module { }
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val violations = findings.filter { it.issue.id == "KtorApplicationKoinInit" }
        assertThat(violations)
            .withFailMessage("Should detect KtorApplicationKoinInit violation")
            .isNotEmpty()
    }

    @Test
    fun `detects KtorRouteScopeMisuse violations`() {
        val code = """
            package com.example.app

            import io.ktor.server.application.*
            import io.ktor.server.routing.*
            import org.koin.core.scope.Scope

            // BAD: Shared scope across requests
            val sharedScope = koinScope()

            fun Application.module() {
                routing {
                    get("/api") {
                        // Using shared scope across requests
                        val service = sharedScope.get<Service>()
                    }
                }
            }

            class Service
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val violations = findings.filter { it.issue.id == "KtorRouteScopeMisuse" }
        assertThat(violations)
            .withFailMessage("Should detect KtorRouteScopeMisuse violation")
            .isNotEmpty()
    }

    @Test
    fun `detects KtorRequestScopeMisuse violations`() {
        val code = """
            package com.example.app

            import io.ktor.server.application.*
            import org.koin.dsl.module

            fun Application.module() {
                // BAD: single inside requestScope
                requestScope {
                    single { RequestLogger() }
                }
            }

            class RequestLogger
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val violations = findings.filter { it.issue.id == "KtorRequestScopeMisuse" }
        assertThat(violations)
            .withFailMessage("Should detect KtorRequestScopeMisuse violation")
            .isNotEmpty()
    }

    @Test
    fun `does not report false positives on correct Ktor code`() {
        val code = """
            package com.example.app

            import io.ktor.server.application.*
            import io.ktor.server.routing.*
            import org.koin.ktor.plugin.Koin
            import org.koin.dsl.module

            // GOOD: install(Koin) at Application level
            fun Application.module() {
                install(Koin) {
                    modules(appModule)
                }
                routing {
                    get("/api") {
                        // GOOD: call.koinScope() for request-scoped
                        val service = call.koinScope().get<Service>()
                    }
                }
            }

            val appModule = module {
                single { Service() }
            }

            class Service
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val ktorViolations = findings.filter { finding ->
            finding.issue.id in listOf(
                "KtorApplicationKoinInit",
                "KtorRouteScopeMisuse",
                "KtorRequestScopeMisuse"
            )
        }
        assertThat(ktorViolations)
            .withFailMessage("Should not report false positives: ${ktorViolations.map { it.issue.id }}")
            .isEmpty()
    }

    @Test
    fun `detects multiple violations in complex Ktor scenario`() {
        val code = """
            package com.example.app

            import io.ktor.server.application.*
            import io.ktor.server.routing.*
            import org.koin.ktor.plugin.Koin
            import org.koin.core.scope.Scope
            import org.koin.dsl.module

            // BAD 1: Shared scope
            val sharedScope = koinScope()

            fun Application.module() {
                routing {
                    // BAD 2: install(Koin) in routing
                    install(Koin) {
                        modules(appModule)
                    }

                    // BAD 3: Using shared scope
                    get("/api") {
                        val service = sharedScope.get<Service>()
                    }

                    // BAD 4: single in requestScope
                    requestScope {
                        single { RequestLogger() }
                    }
                }
            }

            val appModule = module { }
            class Service
            class RequestLogger
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        assertThat(findings.size)
            .withFailMessage("Expected at least 4 violations, got: ${findings.map { it.issue.id }}")
            .isGreaterThanOrEqualTo(4)

        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).contains(
            "KtorRouteScopeMisuse",
            "KtorApplicationKoinInit",
            "KtorRequestScopeMisuse"
        )
    }
}
