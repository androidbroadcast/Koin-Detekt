package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SingleOnAbstractClassTest {

    @Test
    fun `reports Single on abstract class`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            abstract class BaseRepository
        """.trimIndent()

        val findings = SingleOnAbstractClass(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("abstract")
    }

    @Test
    fun `reports Factory on interface`() {
        val code = """
            import org.koin.core.annotation.Factory

            @Factory
            interface Repository
        """.trimIndent()

        val findings = SingleOnAbstractClass(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("interface")
    }

    @Test
    fun `reports Scoped on abstract class`() {
        val code = """
            import org.koin.core.annotation.Scoped

            @Scoped
            abstract class BaseService
        """.trimIndent()

        val findings = SingleOnAbstractClass(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report Single on concrete class`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class RepositoryImpl : Repository
        """.trimIndent()

        val findings = SingleOnAbstractClass(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report unannotated abstract class`() {
        val code = """
            abstract class BaseClass
        """.trimIndent()

        val findings = SingleOnAbstractClass(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
