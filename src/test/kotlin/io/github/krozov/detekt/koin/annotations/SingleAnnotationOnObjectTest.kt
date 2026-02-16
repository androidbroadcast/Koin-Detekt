package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SingleAnnotationOnObjectTest {

    @Test
    fun `reports Single on object declaration`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            object MySingleton
        """.trimIndent()

        val findings = SingleAnnotationOnObject(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("object declaration")
    }

    @Test
    fun `reports Factory on object declaration`() {
        val code = """
            import org.koin.core.annotation.Factory

            @Factory
            object MyFactory
        """.trimIndent()

        val findings = SingleAnnotationOnObject(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows Single on regular class`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService
        """.trimIndent()

        val findings = SingleAnnotationOnObject(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows object without Koin annotations`() {
        val code = """
            object PlainObject
        """.trimIndent()

        val findings = SingleAnnotationOnObject(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports KoinViewModel on object`() {
        val code = """
            import org.koin.android.annotation.KoinViewModel

            @KoinViewModel
            object BadViewModel
        """.trimIndent()

        val findings = SingleAnnotationOnObject(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows companion object without report`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService {
                companion object
            }
        """.trimIndent()

        val findings = SingleAnnotationOnObject(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
