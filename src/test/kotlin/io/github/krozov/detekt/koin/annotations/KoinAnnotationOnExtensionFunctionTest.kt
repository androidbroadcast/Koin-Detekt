package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KoinAnnotationOnExtensionFunctionTest {

    @Test
    fun `reports Single on extension function`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Single
            import org.koin.core.scope.Scope

            @Module
            class MyModule {
                @Single
                fun Scope.provideDatastore(): DataStore = DataStore()
            }
        """.trimIndent()

        val findings = KoinAnnotationOnExtensionFunction(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("extension function")
    }

    @Test
    fun `reports Factory on extension function`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Factory

            @Module
            class MyModule {
                @Factory
                fun String.toService(): MyService = MyService(this)
            }
        """.trimIndent()

        val findings = KoinAnnotationOnExtensionFunction(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows regular function with annotation`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Single

            @Module
            class MyModule {
                @Single
                fun provideService(): MyService = MyService()
            }
        """.trimIndent()

        val findings = KoinAnnotationOnExtensionFunction(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports Scoped on extension function`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Scoped

            @Module
            class MyModule {
                @Scoped
                fun String.toService(): MyService = MyService(this)
            }
        """.trimIndent()

        val findings = KoinAnnotationOnExtensionFunction(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports extension function with generic receiver`() {
        val code = """
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Single

            @Module
            class MyModule {
                @Single
                fun List<String>.toService(): MyService = MyService()
            }
        """.trimIndent()

        val findings = KoinAnnotationOnExtensionFunction(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("List<String>")
    }

    @Test
    fun `allows extension function without Koin annotations`() {
        val code = """
            fun String.toUpperCase(): String = this.uppercase()
        """.trimIndent()

        val findings = KoinAnnotationOnExtensionFunction(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
