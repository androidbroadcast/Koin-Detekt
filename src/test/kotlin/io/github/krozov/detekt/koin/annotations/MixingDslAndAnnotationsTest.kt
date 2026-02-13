package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MixingDslAndAnnotationsTest {

    @Test
    fun `reports mixing DSL and Annotations in same file`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Single
            import org.koin.dsl.module

            @Module
            class AnnotatedModule {
                @Single
                fun provideRepo(): Repository = RepositoryImpl()
            }

            val dslModule = module {
                single { ApiService() }
            }
        """.trimIndent()

        val findings = MixingDslAndAnnotations(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("mixing")
        assertThat(findings[0].message).contains("DSL")
        assertThat(findings[0].message).contains("Annotations")
    }

    @Test
    fun `allows DSL only`() {
        val code = """
            import org.koin.dsl.module

            val module = module {
                single { Service() }
            }
        """.trimIndent()

        val findings = MixingDslAndAnnotations(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows Annotations only`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Single

            @Module
            class MyModule {
                @Single
                fun provideService(): Service = ServiceImpl()
            }
        """.trimIndent()

        val findings = MixingDslAndAnnotations(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
