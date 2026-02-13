package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MissingModuleAnnotationTest {

    @Test
    fun `reports Single without Module annotation`() {
        val code = """
            import org.koin.core.annotation.Single

            class MyServices {
                @Single
                fun provideRepo(): Repository = RepositoryImpl()
            }
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("@Module")
        assertThat(findings[0].message).contains("@Single")
    }

    @Test
    fun `allows Single with Module annotation`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Single

            @Module
            class MyServices {
                @Single
                fun provideRepo(): Repository = RepositoryImpl()
            }
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports Factory without Module annotation`() {
        val code = """
            import org.koin.core.annotation.Factory

            class MyServices {
                @Factory
                fun createService(): Service = ServiceImpl()
            }
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows class without Koin annotations`() {
        val code = """
            class MyServices {
                fun provideRepo(): Repository = RepositoryImpl()
            }
        """.trimIndent()

        val findings = MissingModuleAnnotation(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
