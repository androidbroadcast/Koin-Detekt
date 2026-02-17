package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InvalidNamedQualifierCharactersTest {

    @Test
    fun `reports Named with hyphen`() {
        val code = """
            import org.koin.core.annotation.Named
            import org.koin.core.annotation.Single

            @Single
            @Named("ricky-morty")
            class MyService
        """.trimIndent()

        val findings = InvalidNamedQualifierCharacters(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("ricky-morty")
    }

    @Test
    fun `reports Named with spaces`() {
        val code = """
            import org.koin.core.annotation.Named
            import org.koin.core.annotation.Single

            @Single
            @Named("my service")
            class MyService
        """.trimIndent()

        val findings = InvalidNamedQualifierCharacters(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows Named with valid characters`() {
        val code = """
            import org.koin.core.annotation.Named
            import org.koin.core.annotation.Single

            @Single
            @Named("myService")
            class MyService
        """.trimIndent()

        val findings = InvalidNamedQualifierCharacters(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows Named with underscores and dots`() {
        val code = """
            import org.koin.core.annotation.Named
            import org.koin.core.annotation.Single

            @Single
            @Named("my_service.impl")
            class MyService
        """.trimIndent()

        val findings = InvalidNamedQualifierCharacters(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows Named with only digits and letters`() {
        val code = """
            import org.koin.core.annotation.Named
            import org.koin.core.annotation.Single

            @Single
            @Named("service123")
            class MyService
        """.trimIndent()

        val findings = InvalidNamedQualifierCharacters(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports Named starting with digit`() {
        val code = """
            import org.koin.core.annotation.Named
            import org.koin.core.annotation.Single

            @Single
            @Named("123abc")
            class MyService
        """.trimIndent()

        val findings = InvalidNamedQualifierCharacters(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `ignores non-Named annotations`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            @Deprecated("use other")
            class MyService
        """.trimIndent()

        val findings = InvalidNamedQualifierCharacters(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `ignores Named without arguments`() {
        val code = """
            import org.koin.core.annotation.Named
            import org.koin.core.annotation.Single

            @Single
            @Named
            class MyService
        """.trimIndent()

        val findings = InvalidNamedQualifierCharacters(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `ignores Named with constant reference`() {
        val code = """
            import org.koin.core.annotation.Named
            import org.koin.core.annotation.Single

            @Single
            @Named(QUALIFIER_NAME)
            class MyService
        """.trimIndent()

        val findings = InvalidNamedQualifierCharacters(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports Named on function parameter`() {
        val code = """
            import org.koin.core.annotation.Named
            import org.koin.core.annotation.Module
            import org.koin.core.annotation.Single

            @Module
            class MyModule {
                @Single
                fun provideService(@Named("bad-name") dep: Dependency): MyService = MyService(dep)
            }
        """.trimIndent()

        val findings = InvalidNamedQualifierCharacters(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
