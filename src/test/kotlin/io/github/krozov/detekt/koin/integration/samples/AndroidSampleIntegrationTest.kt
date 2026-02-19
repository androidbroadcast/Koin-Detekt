package io.github.krozov.detekt.koin.integration.samples

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Integration tests verifying Detekt rules work correctly on real Android sample code.
 * These tests ensure the rules catch violations in realistic Android scenarios.
 */
class AndroidSampleIntegrationTest {

    private val ruleSet = KoinRuleSetProvider().instance(Config.empty)

    @Test
    fun `detects ActivityFragmentKoinScope violations in Android code`() {
        val code = """
            package com.example.app

            import androidx.fragment.app.Fragment
            import org.koin.android.ext.android.get
            import org.koin.androidx.scope.activityScope

            class MyFragment : Fragment() {
                // BAD: Using activityScope in Fragment
                private val vm by activityScope().get<MyViewModel>()
            }

            class MyViewModel
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val scopeViolations = findings.filter { it.issue.id == "ActivityFragmentKoinScope" }
        assertThat(scopeViolations)
            .withFailMessage("Should detect ActivityFragmentKoinScope violation")
            .isNotEmpty()
    }

    @Test
    fun `detects AndroidContextNotFromKoin violations`() {
        val code = """
            package com.example.app

            import org.koin.android.ext.koin.androidContext

            // BAD: androidContext() outside startKoin or module context
            class MyService {
                val context = androidContext()
            }
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val contextViolations = findings.filter { it.issue.id == "AndroidContextNotFromKoin" }
        assertThat(contextViolations)
            .withFailMessage("Should detect AndroidContextNotFromKoin violation")
            .isNotEmpty()
    }

    @Test
    fun `detects StartKoinInActivity violations`() {
        val code = """
            package com.example.app

            import android.app.Activity
            import org.koin.android.ext.koin.androidContext
            import org.koin.core.context.startKoin

            // BAD: Starting Koin in Activity
            class MainActivity : Activity() {
                override fun onCreate() {
                    super.onCreate()
                    startKoin {
                        androidContext(this@MainActivity)
                        modules(appModule)
                    }
                }
            }

            val appModule = module { }
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val startKoinViolations = findings.filter { it.issue.id == "StartKoinInActivity" }
        assertThat(startKoinViolations)
            .withFailMessage("Should detect StartKoinInActivity violation")
            .isNotEmpty()
    }

    @Test
    fun `does not report false positives on correct Android code`() {
        val code = """
            package com.example.app

            import android.app.Application
            import androidx.fragment.app.Fragment
            import org.koin.android.ext.koin.androidContext
            import org.koin.androidx.scope.fragmentScope
            import org.koin.androidx.viewmodel.dsl.viewModel
            import org.koin.core.context.startKoin
            import org.koin.dsl.module

            // GOOD: Starting Koin in Application
            class MyApp : Application() {
                override fun onCreate() {
                    super.onCreate()
                    startKoin {
                        androidContext(this@MyApp)
                        modules(appModule)
                    }
                }
            }

            val appModule = module {
                viewModel { MyViewModel() }
            }

            // GOOD: Using fragmentScope in Fragment
            class MyFragment : Fragment() {
                private val vm by fragmentScope().get<MyViewModel>()
            }

            class MyViewModel
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // Should not have false positive violations
        val androidViolations = findings.filter { finding ->
            finding.issue.id in listOf(
                "ActivityFragmentKoinScope",
                "AndroidContextNotFromKoin",
                "StartKoinInActivity"
            )
        }
        assertThat(androidViolations)
            .withFailMessage("Should not report false positives: ${androidViolations.map { it.issue.id }}")
            .isEmpty()
    }

    @Test
    fun `detects multiple violations in complex Android scenario`() {
        val code = """
            package com.example.app

            import android.app.Activity
            import androidx.fragment.app.Fragment
            import org.koin.android.ext.koin.androidContext
            import org.koin.androidx.scope.activityScope
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get
            import org.koin.core.context.startKoin
            import org.koin.dsl.module

            // BAD: Starting Koin in Activity
            class MainActivity : Activity() {
                override fun onCreate() {
                    super.onCreate()
                    startKoin {
                        androidContext(this@MainActivity)
                    }
                }
            }

            // BAD: KoinComponent in non-framework class
            class MyRepository : KoinComponent {
                private val api = get<ApiService>()
            }

            interface ApiService

            // BAD: activityScope in Fragment
            class MyFragment : Fragment() {
                private val vm by activityScope().get<MyViewModel>()
            }

            class MyViewModel
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        // Should detect at least 3 violations
        assertThat(findings.size)
            .withFailMessage("Expected at least 3 violations, got: ${findings.map { it.issue.id }}")
            .isGreaterThanOrEqualTo(3)

        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).contains(
            "StartKoinInActivity",
            "NoKoinComponentInterface",
            "NoGetOutsideModuleDefinition",
            "ActivityFragmentKoinScope"
        )
    }
}
