package io.github.krozov.detekt.koin.platform

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StartKoinInActivityTest {

    @Test
    fun `reports startKoin in Activity onCreate`() {
        val code = """
            import android.app.Activity
            import org.koin.core.context.startKoin

            class MainActivity : Activity() {
                override fun onCreate() {
                    startKoin { }
                }
            }
        """.trimIndent()

        val findings = StartKoinInActivity(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("Activity/Fragment")
        assertThat(findings[0].message).contains("KoinAppAlreadyStartedException")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports startKoin in Fragment onViewCreated`() {
        val code = """
            import androidx.fragment.app.Fragment
            import org.koin.core.context.startKoin

            class MyFragment : Fragment() {
                override fun onViewCreated() {
                    startKoin { }
                }
            }
        """.trimIndent()

        val findings = StartKoinInActivity(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports startKoin in Composable function`() {
        val code = """
            import androidx.compose.runtime.Composable
            import org.koin.core.context.startKoin

            @Composable
            fun MyScreen() {
                startKoin { }
            }
        """.trimIndent()

        val findings = StartKoinInActivity(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report startKoin in Application`() {
        val code = """
            import android.app.Application
            import org.koin.core.context.startKoin

            class MyApp : Application() {
                override fun onCreate() {
                    startKoin { }
                }
            }
        """.trimIndent()

        val findings = StartKoinInActivity(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report startKoin in plain class`() {
        val code = """
            import org.koin.core.context.startKoin

            class PlainClass {
                fun init() {
                    startKoin { }
                }
            }
        """.trimIndent()

        val findings = StartKoinInActivity(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report startKoin in function`() {
        val code = """
            import org.koin.core.context.startKoin

            fun initKoin() {
                startKoin { }
            }
        """.trimIndent()

        val findings = StartKoinInActivity(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports startKoin in Activity subclass`() {
        val code = """
            import androidx.appcompat.app.AppCompatActivity
            import org.koin.core.context.startKoin

            class MainActivity : AppCompatActivity() {
                override fun onCreate() {
                    startKoin { }
                }
            }
        """.trimIndent()

        val findings = StartKoinInActivity(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report other calls in Activity`() {
        val code = """
            import android.app.Activity
            import org.koin.core.context.loadKoinModules

            class MainActivity : Activity() {
                override fun onCreate() {
                    loadKoinModules(myModule)
                }
            }
        """.trimIndent()

        val findings = StartKoinInActivity(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
