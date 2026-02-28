package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ScopeDeclareWithActivityOrFragmentTest {

    @Test
    fun `reports scope declare with activity parameter`() {
        val code = """
            import org.koin.core.scope.Scope
            import android.app.Activity

            fun setupScope(scope: Scope, activity: Activity) {
                scope.declare(activity)
            }
        """.trimIndent()

        val findings = ScopeDeclareWithActivityOrFragment(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("declare(activity/fragment)")
        assertThat(findings[0].message).contains("Memory leak")
    }

    @Test
    fun `reports scope declare with fragment parameter`() {
        val code = """
            import org.koin.core.scope.Scope
            import androidx.fragment.app.Fragment

            fun setupScope(scope: Scope, fragment: Fragment) {
                scope.declare(fragment)
            }
        """.trimIndent()

        val findings = ScopeDeclareWithActivityOrFragment(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports scope declare with activity variable name`() {
        val code = """
            import org.koin.core.scope.Scope

            fun setupScope(scope: Scope, myActivity: Any) {
                scope.declare(myActivity)
            }
        """.trimIndent()

        val findings = ScopeDeclareWithActivityOrFragment(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports scope declare with fragment variable name`() {
        val code = """
            import org.koin.core.scope.Scope

            fun setupScope(scope: Scope, myFragment: Any) {
                scope.declare(myFragment)
            }
        """.trimIndent()

        val findings = ScopeDeclareWithActivityOrFragment(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report scope declare with other objects`() {
        val code = """
            import org.koin.core.scope.Scope

            fun setupScope(scope: Scope, service: Any) {
                scope.declare(service)
            }
        """.trimIndent()

        val findings = ScopeDeclareWithActivityOrFragment(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report other scope methods`() {
        val code = """
            import org.koin.core.scope.Scope
            import android.app.Activity

            fun setupScope(scope: Scope, activity: Activity) {
                scope.get<MyService>()
            }
        """.trimIndent()

        val findings = ScopeDeclareWithActivityOrFragment(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports scope declare with AppCompatActivity`() {
        val code = """
            import org.koin.core.scope.Scope
            import androidx.appcompat.app.AppCompatActivity

            fun setupScope(scope: Scope, activity: AppCompatActivity) {
                scope.declare(activity)
            }
        """.trimIndent()

        val findings = ScopeDeclareWithActivityOrFragment(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports scope declare with this in Activity`() {
        val code = """
            import androidx.appcompat.app.AppCompatActivity
            import org.koin.core.scope.Scope

            class MainActivity : AppCompatActivity() {
                fun setup(scope: Scope) {
                    scope.declare(this)
                }
            }
        """.trimIndent()

        val findings = ScopeDeclareWithActivityOrFragment(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `case insensitive detection of activity keyword`() {
        val code = """
            import org.koin.core.scope.Scope

            fun setupScope(scope: Scope, mainActivity: Any) {
                scope.declare(mainActivity)
            }
        """.trimIndent()

        val findings = ScopeDeclareWithActivityOrFragment(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports scope declare with additional leak-prone type via config`() {
        val code = """
            import org.koin.core.scope.Scope

            fun setupScope(scope: Scope, presenter: Any) {
                scope.declare(presenter)
            }
        """.trimIndent()

        val config = TestConfig("additionalLeakProneTypes" to listOf("Presenter"))
        val findings = ScopeDeclareWithActivityOrFragment(config).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report scope declare with unrecognized type when no additional config`() {
        val code = """
            import org.koin.core.scope.Scope

            fun setupScope(scope: Scope, presenter: Any) {
                scope.declare(presenter)
            }
        """.trimIndent()

        val findings = ScopeDeclareWithActivityOrFragment(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports scope declare this when containing class extends additional leak-prone supertype`() {
        val code = """
            import org.koin.core.scope.Scope

            open class Presenter

            class MyPresenter : Presenter() {
                fun setup(scope: Scope) {
                    scope.declare(this)
                }
            }
        """.trimIndent()

        val config = TestConfig("additionalLeakProneTypes" to listOf("Presenter"))
        val findings = ScopeDeclareWithActivityOrFragment(config).lint(code)
        assertThat(findings).hasSize(1)
    }
}
