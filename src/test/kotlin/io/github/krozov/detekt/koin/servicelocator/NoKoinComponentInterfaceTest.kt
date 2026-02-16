package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NoKoinComponentInterfaceTest {

    @Test
    fun `reports KoinComponent in regular class`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class UserRepository : KoinComponent {
                fun getData() = get<ApiService>()
            }
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("KoinComponent")
    }

    @Test
    fun `reports KoinScopeComponent in regular class`() {
        val code = """
            import org.koin.core.component.KoinScopeComponent

            class MyService : KoinScopeComponent {
                override val scope = getKoin().createScope()
            }
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report when extending allowed super type`() {
        val config = io.gitlab.arturbosch.detekt.test.TestConfig(
            "allowedSuperTypes" to listOf("Application", "Activity")
        )

        val code = """
            import org.koin.core.component.KoinComponent

            class MainActivity : Activity(), KoinComponent
        """.trimIndent()

        val findings = NoKoinComponentInterface(config)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report class without KoinComponent`() {
        val code = """
            class MyService(private val repo: Repository)
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports edge case - NonActivity should not match Activity`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class NonActivity : KoinComponent
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("KoinComponent")
        assertThat(findings[0].message).contains("→")
    }

    @Test
    fun `does not report ViewModel with generic parameter`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class MyViewModel<T> : ViewModel(), KoinComponent
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `recognizes fully qualified ComponentActivity`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class MyActivity : androidx.activity.ComponentActivity(), KoinComponent
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report Activity with multiple interfaces`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class MyActivity : Activity(), SomeInterface, KoinComponent
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports KoinComponent in companion object`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class MyClass {
                companion object : KoinComponent
            }
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports with enhanced message format`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class MyClass : KoinComponent
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports KoinComponent in regular object declaration`() {
        val code = """
            import org.koin.core.component.KoinComponent

            object MySingleton : KoinComponent {
                fun getData() = get<Service>()
            }
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("KoinComponent")
    }

    @Test
    fun `does not report object with allowed super type`() {
        val code = """
            import org.koin.core.component.KoinComponent

            object MyApp : Application(), KoinComponent
        """.trimIndent()

        val findings = NoKoinComponentInterface(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
