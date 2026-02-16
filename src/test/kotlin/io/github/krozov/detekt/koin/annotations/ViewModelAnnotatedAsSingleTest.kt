package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ViewModelAnnotatedAsSingleTest {

    @Test
    fun `reports ViewModel with Single annotation`() {
        val code = """
            import org.koin.core.annotation.Single
            import androidx.lifecycle.ViewModel

            @Single
            class MyViewModel : ViewModel()
        """.trimIndent()

        val findings = ViewModelAnnotatedAsSingle(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("@KoinViewModel")
    }

    @Test
    fun `reports AndroidViewModel with Single annotation`() {
        val code = """
            import org.koin.core.annotation.Single
            import androidx.lifecycle.AndroidViewModel
            import android.app.Application

            @Single
            class MyViewModel(app: Application) : AndroidViewModel(app)
        """.trimIndent()

        val findings = ViewModelAnnotatedAsSingle(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports ViewModel with Factory annotation`() {
        val code = """
            import org.koin.core.annotation.Factory
            import androidx.lifecycle.ViewModel

            @Factory
            class MyViewModel : ViewModel()
        """.trimIndent()

        val findings = ViewModelAnnotatedAsSingle(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows ViewModel with KoinViewModel annotation`() {
        val code = """
            import org.koin.android.annotation.KoinViewModel
            import androidx.lifecycle.ViewModel

            @KoinViewModel
            class MyViewModel : ViewModel()
        """.trimIndent()

        val findings = ViewModelAnnotatedAsSingle(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports class extending base ViewModel via heuristic`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyViewModel : BaseViewModel()
        """.trimIndent()

        val findings = ViewModelAnnotatedAsSingle(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows regular class with Single annotation`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService
        """.trimIndent()

        val findings = ViewModelAnnotatedAsSingle(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
