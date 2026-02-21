package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NoInjectDelegateTest {

    @Test
    fun `reports inject() delegate usage in regular class`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.inject

            class MyRepository : KoinComponent {
                val api: ApiService by inject()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `does not report constructor injection`() {
        val code = """
            class MyRepository(
                private val api: ApiService
            )
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report regular by lazy`() {
        val code = """
            class MyRepository {
                val api: ApiService by lazy { ApiServiceImpl() }
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports injectOrNull() delegate usage`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.injectOrNull

            class MyRepository : KoinComponent {
                val api: ApiService? by injectOrNull()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports inject in companion object`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.inject

            class MyClass : KoinComponent {
                companion object {
                    val repo: Repository by inject()
                }
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports multiple inject delegates`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.inject

            class MyClass : KoinComponent {
                val repo: Repository by inject()
                val api: ApiService by inject()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty).lint(code)
        assertThat(findings).hasSize(2)
    }

    @Test
    fun `reports lazy inject delegate`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.inject

            class MyClass : KoinComponent {
                val repo by inject<Repository>()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    // Allowed super type tests
    @Test
    fun `allows inject in Activity`() {
        val code = """
            import org.koin.core.component.inject

            class MyActivity : Activity() {
                val repo: Repository by inject()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows inject in Fragment`() {
        val code = """
            import org.koin.core.component.inject

            class MyFragment : Fragment() {
                val repo: Repository by inject()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows inject in Application`() {
        val code = """
            import org.koin.core.component.inject

            class MyApp : Application() {
                val repo: Repository by inject()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows inject in ViewModel`() {
        val code = """
            import org.koin.core.component.inject

            class MyViewModel : ViewModel() {
                val repo: Repository by inject()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports inject in plain class even with KoinComponent`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.inject

            class PlainService : KoinComponent {
                val repo: Repository by inject()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows inject with custom allowed super type config`() {
        val config = TestConfig("allowedSuperTypes" to listOf("Worker"))
        val code = """
            import org.koin.core.component.inject

            class MyWorker : Worker() {
                val repo: Repository by inject()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(config).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports inject in Activity when allowedSuperTypes is empty`() {
        val config = TestConfig("allowedSuperTypes" to emptyList<String>())
        val code = """
            import org.koin.core.component.inject

            class MyActivity : Activity() {
                val repo: Repository by inject()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(config).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows inject in ComponentActivity`() {
        val code = """
            import org.koin.core.component.inject

            class MyActivity : ComponentActivity() {
                val repo: Repository by inject()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows inject in class with generic super type`() {
        val code = """
            import org.koin.core.component.inject

            class MyFragment : Fragment<MyBinding>() {
                val repo: Repository by inject()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report inject delegate in GlanceAppWidget`() {
        val code = """
            import androidx.glance.appwidget.GlanceAppWidget
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.inject

            class MyWidget : GlanceAppWidget(), KoinComponent {
                private val repo: MyRepo by inject()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report inject delegate in GlanceAppWidgetReceiver`() {
        val code = """
            import androidx.glance.appwidget.GlanceAppWidgetReceiver
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.inject

            class MyWidgetReceiver : GlanceAppWidgetReceiver(), KoinComponent {
                private val repo: MyRepo by inject()
            }
        """.trimIndent()

        val findings = NoInjectDelegate(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
