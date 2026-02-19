package io.github.krozov.detekt.koin.integration.samples

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Integration tests verifying Detekt rules work correctly on Jetpack Compose sample code.
 */
class ComposeSampleIntegrationTest {

    private val ruleSet = KoinRuleSetProvider().instance(Config.empty)

    @Test
    fun `detects KoinViewModelOutsideComposable violations`() {
        val code = """
            package com.example.app.ui

            import androidx.compose.runtime.Composable
            import org.koin.androidx.compose.koinViewModel

            // BAD: koinViewModel outside @Composable
            fun MyScreen() {
                val vm = koinViewModel<MyViewModel>()
            }

            class MyViewModel
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val violations = findings.filter { it.issue.id == "KoinViewModelOutsideComposable" }
        assertThat(violations)
            .withFailMessage("Should detect KoinViewModelOutsideComposable violation")
            .isNotEmpty()
    }

    @Test
    fun `detects KoinInjectInPreview violations`() {
        val code = """
            package com.example.app.ui

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.tooling.preview.Preview
            import org.koin.androidx.compose.koinInject

            @Preview
            @Composable
            fun MyScreenPreview() {
                // BAD: koinInject in Preview
                val repo = koinInject<Repository>()
                MyScreen(repo)
            }

            @Composable
            fun MyScreen(repo: Repository) {}

            class Repository
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val violations = findings.filter { it.issue.id == "KoinInjectInPreview" }
        assertThat(violations)
            .withFailMessage("Should detect KoinInjectInPreview violation")
            .isNotEmpty()
    }

    @Test
    fun `detects RememberKoinModulesLeak violations`() {
        val code = """
            package com.example.app.ui

            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import org.koin.core.context.loadKoinModules
            import org.koin.dsl.module

            val featureModule = module { }

            @Composable
            fun FeatureScreen() {
                // BAD: loadKoinModules in remember without unload
                remember { loadKoinModules(featureModule) }
            }
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val violations = findings.filter { it.issue.id == "RememberKoinModulesLeak" }
        assertThat(violations)
            .withFailMessage("Should detect RememberKoinModulesLeak violation")
            .isNotEmpty()
    }

    @Test
    fun `does not report false positives on correct Compose code`() {
        val code = """
            package com.example.app.ui

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.tooling.preview.Preview
            import androidx.compose.runtime.DisposableEffect
            import org.koin.androidx.compose.koinInject
            import org.koin.androidx.compose.koinViewModel
            import org.koin.core.context.loadKoinModules
            import org.koin.core.context.unloadKoinModules
            import org.koin.dsl.module

            val featureModule = module { }

            // GOOD: koinViewModel in @Composable
            @Composable
            fun MyScreen() {
                val vm = koinViewModel<MyViewModel>()
            }

            // GOOD: DisposableEffect pattern for module loading
            @Composable
            fun FeatureScreen() {
                DisposableEffect(Unit) {
                    loadKoinModules(featureModule)
                    onDispose { unloadKoinModules(featureModule) }
                }
            }

            // GOOD: Preview with fake dependencies
            @Preview
            @Composable
            fun MyScreenPreview() {
                MyScreen(FakeRepository())
            }

            @Composable
            fun MyScreen(repo: Repository) {}

            class MyViewModel
            class Repository
            class FakeRepository : Repository
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val composeViolations = findings.filter { finding ->
            finding.issue.id in listOf(
                "KoinViewModelOutsideComposable",
                "KoinInjectInPreview",
                "RememberKoinModulesLeak"
            )
        }
        assertThat(composeViolations)
            .withFailMessage("Should not report false positives: ${composeViolations.map { it.issue.id }}")
            .isEmpty()
    }

    @Test
    fun `detects violations in complex Compose scenario`() {
        val code = """
            package com.example.app.ui

            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.ui.tooling.preview.Preview
            import org.koin.androidx.compose.koinInject
            import org.koin.androidx.compose.koinViewModel
            import org.koin.core.context.loadKoinModules
            import org.koin.dsl.module

            val featureModule = module { }

            // BAD 1: koinViewModel outside @Composable
            fun MyScreen() {
                val vm = koinViewModel<MyViewModel>()
            }

            // BAD 2: koinInject in Preview
            @Preview
            @Composable
            fun MyScreenPreview() {
                val repo = koinInject<Repository>()
                MyScreen(repo)
            }

            // BAD 3: Remember leak
            @Composable
            fun FeatureScreen() {
                remember { loadKoinModules(featureModule) }
            }

            @Composable
            fun MyScreen(repo: Repository) {}

            class MyViewModel
            class Repository
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        assertThat(findings.size)
            .withFailMessage("Expected at least 3 violations, got: ${findings.map { it.issue.id }}")
            .isGreaterThanOrEqualTo(3)

        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).contains(
            "KoinViewModelOutsideComposable",
            "KoinInjectInPreview",
            "RememberKoinModulesLeak"
        )
    }
}
