package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConflictingBindingsTest {

    @Test
    fun `reports same type in DSL and Annotations`() {
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
                single<Repository> { RepositoryImpl() }
            }
        """.trimIndent()

        val findings = ConflictingBindings(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `allows different types`() {
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

        val findings = ConflictingBindings(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports duplicate definition annotations on same class`() {
        val code = """
            import org.koin.core.annotation.Single
            import org.koin.core.annotation.Factory

            @Single
            @Factory
            class MyService
        """.trimIndent()

        val findings = ConflictingBindings(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("@Single")
        assertThat(findings[0].message).contains("@Factory")
    }

    @Test
    fun `allows single definition annotation`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService
        """.trimIndent()

        val findings = ConflictingBindings(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows same type with different qualifiers`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Single
            import org.koin.core.annotation.Named
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            @Module
            class AnnotatedModule {
                @Single
                @Named("impl1")
                fun provideRepo1(): Repository = RepositoryImpl1()
            }

            val dslModule = module {
                single(named("impl2")) { Repository2() as Repository }
            }
        """.trimIndent()

        val findings = ConflictingBindings(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
