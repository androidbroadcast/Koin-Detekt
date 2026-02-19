package io.github.krozov.detekt.koin.integration.edgecases

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Integration tests verifying platform-specific scenarios.
 */
class PlatformSpecificIntegrationTest {

    private val ruleSet = KoinRuleSetProvider().instance(Config.empty)

    @Test
    fun `handles Android-specific API without crashing`() {
        val code = """
            package com.example

            import android.app.Application
            import android.content.Context
            import androidx.fragment.app.Fragment
            import androidx.lifecycle.ViewModel
            import org.koin.android.ext.koin.androidContext
            import org.koin.androidx.viewmodel.dsl.viewModel
            import org.koin.dsl.module

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

            class MyViewModel : ViewModel()

            class MyFragment : Fragment()
        """.trimIndent()

        // Should handle Android API without crashing
        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }
        assertThat(findings).isNotNull()
    }

    @Test
    fun `handles Ktor-specific API without crashing`() {
        val code = """
            package com.example

            import io.ktor.server.application.*
            import io.ktor.server.routing.*
            import org.koin.ktor.plugin.Koin
            import org.koin.dsl.module

            fun Application.module() {
                install(Koin) {
                    modules(appModule)
                }
                routing {
                    get("/api") {
                        call.koinScope().get<Service>()
                    }
                }
            }

            val appModule = module {
                single { Service() }
            }

            class Service
        """.trimIndent()

        // Should handle Ktor API without crashing
        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }
        assertThat(findings).isNotNull()
    }

    @Test
    fun `handles Compose-specific API without crashing`() {
        val code = """
            package com.example

            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.DisposableEffect
            import androidx.compose.runtime.remember
            import org.koin.androidx.compose.koinInject
            import org.koin.androidx.compose.koinViewModel
            import org.koin.core.context.loadKoinModules
            import org.koin.core.context.unloadKoinModules
            import org.koin.dsl.module

            val featureModule = module { }

            @Composable
            fun MyScreen() {
                val vm = koinViewModel<MyViewModel>()
                val service = koinInject<Service>()

                DisposableEffect(Unit) {
                    loadKoinModules(featureModule)
                    onDispose { unloadKoinModules(featureModule) }
                }
            }

            class MyViewModel
            class Service
        """.trimIndent()

        // Should handle Compose API without crashing
        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }
        assertThat(findings).isNotNull()
    }

    @Test
    fun `handles multiplatform expect actual declarations`() {
        val code = """
            package com.example

            import org.koin.core.module.Module

            expect val platformModule: Module

            data class PlatformConfig(val name: String)
        """.trimIndent()

        // Should handle expect/actual without crashing
        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }
        assertThat(findings).isNotNull()
    }

    @Test
    fun `handles actual declarations`() {
        val code = """
            package com.example

            import org.koin.dsl.module

            actual val platformModule = module {
                single { PlatformConfig("Android") }
            }

            actual data class PlatformConfig(val name: String)
        """.trimIndent()

        // Should handle actual without crashing
        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }
        assertThat(findings).isNotNull()
    }

    @Test
    fun `handles Android-specific scope APIs`() {
        val code = """
            package com.example

            import androidx.fragment.app.Fragment
            import androidx.activity.ComponentActivity
            import org.koin.androidx.scope.activityScope
            import org.koin.androidx.scope.fragmentScope

            class MyActivity : ComponentActivity() {
                private val scope = activityScope
            }

            class MyFragment : Fragment() {
                private val scope = fragmentScope
            }
        """.trimIndent()

        // Should handle Android scope APIs without crashing
        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }
        assertThat(findings).isNotNull()
    }

    @Test
    fun `handles Ktor request scope API`() {
        val code = """
            package com.example

            import io.ktor.server.application.*
            import org.koin.dsl.module

            fun Application.module() {
                requestScope {
                    scoped { RequestService() }
                }
            }

            class RequestService
        """.trimIndent()

        // Should handle requestScope without crashing
        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }
        assertThat(findings).isNotNull()
    }

    @Test
    fun `handles Compose preview API`() {
        val code = """
            package com.example

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.tooling.preview.Preview

            @Preview
            @Composable
            fun MyScreenPreview() {
                MyScreen(FakeService())
            }

            @Composable
            fun MyScreen(service: Service) {}

            class Service
            class FakeService : Service
        """.trimIndent()

        // Should handle Preview without crashing
        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }
        assertThat(findings).isNotNull()
    }

    @Test
    fun `detects platform-specific violations`() {
        val code = """
            package com.example

            import androidx.fragment.app.Fragment
            import org.koin.androidx.scope.activityScope

            class MyFragment : Fragment() {
                // Violation: ActivityFragmentKoinScope - activityScope() called in Fragment
                private val scope = activityScope()
            }
        """.trimIndent()

        val findings = ruleSet.rules.flatMap { rule -> rule.lint(code) }

        val ruleIds = findings.map { it.issue.id }
        assertThat(ruleIds).contains("ActivityFragmentKoinScope")
    }
}