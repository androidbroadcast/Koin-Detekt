package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KoinViewModelOnNonViewModelTest {

    @Test
    fun `reports KoinViewModel on class without ViewModel supertype`() {
        val code = """
            import org.koin.core.annotation.KoinViewModel

            @KoinViewModel
            class MyPresenter
        """.trimIndent()

        val findings = KoinViewModelOnNonViewModel(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("ViewModel")
    }

    @Test
    fun `does not report KoinViewModel on ViewModel subclass`() {
        val code = """
            import org.koin.core.annotation.KoinViewModel
            import androidx.lifecycle.ViewModel

            @KoinViewModel
            class MyViewModel : ViewModel()
        """.trimIndent()

        val findings = KoinViewModelOnNonViewModel(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report KoinViewModel on AndroidViewModel subclass`() {
        val code = """
            import org.koin.core.annotation.KoinViewModel
            import androidx.lifecycle.AndroidViewModel

            @KoinViewModel
            class MyViewModel(app: android.app.Application) : AndroidViewModel(app)
        """.trimIndent()

        val findings = KoinViewModelOnNonViewModel(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report KoinViewModel on custom ViewModel base`() {
        val code = """
            import org.koin.core.annotation.KoinViewModel

            @KoinViewModel
            class MyViewModel : BaseViewModel()
        """.trimIndent()

        val findings = KoinViewModelOnNonViewModel(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report class without KoinViewModel annotation`() {
        val code = """
            class MyPresenter
        """.trimIndent()

        val findings = KoinViewModelOnNonViewModel(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report KoinViewModel when base class is in additionalViewModelSuperTypes`() {
        val code = """
            import org.koin.core.annotation.KoinViewModel

            @KoinViewModel
            class MyPresenter : RxModel()
        """.trimIndent()

        val config = TestConfig("additionalViewModelSuperTypes" to listOf("RxModel"))
        val findings = KoinViewModelOnNonViewModel(config).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports KoinViewModel when base class is not in additionalViewModelSuperTypes`() {
        val code = """
            import org.koin.core.annotation.KoinViewModel

            @KoinViewModel
            class MyPresenter : RxModel()
        """.trimIndent()

        val findings = KoinViewModelOnNonViewModel(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report KoinViewModel on class extending ViewModel and interface`() {
        val code = """
            import org.koin.core.annotation.KoinViewModel
            import androidx.lifecycle.ViewModel

            @KoinViewModel
            class MyViewModel : SomeInterface, ViewModel()
        """.trimIndent()

        val findings = KoinViewModelOnNonViewModel(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
