package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ComponentScanPackageMismatchTest {

    @Test
    fun `reports ComponentScan with empty string`() {
        val code = """
            package com.example.feature

            import org.koin.core.annotation.Module
            import org.koin.core.annotation.ComponentScan

            @Module
            @ComponentScan("")
            class FeatureModule
        """.trimIndent()

        val findings = ComponentScanPackageMismatch(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("empty")
    }

    @Test
    fun `reports ComponentScan with unrelated package`() {
        val code = """
            package com.example.feature

            import org.koin.core.annotation.Module
            import org.koin.core.annotation.ComponentScan

            @Module
            @ComponentScan("com.other.module")
            class FeatureModule
        """.trimIndent()

        val findings = ComponentScanPackageMismatch(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("com.other.module")
    }

    @Test
    fun `does not report ComponentScan matching own package`() {
        val code = """
            package com.example.feature

            import org.koin.core.annotation.Module
            import org.koin.core.annotation.ComponentScan

            @Module
            @ComponentScan("com.example.feature")
            class FeatureModule
        """.trimIndent()

        val findings = ComponentScanPackageMismatch(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report ComponentScan with ancestor package`() {
        val code = """
            package com.example.feature.auth

            import org.koin.core.annotation.Module
            import org.koin.core.annotation.ComponentScan

            @Module
            @ComponentScan("com.example.feature")
            class AuthModule
        """.trimIndent()

        val findings = ComponentScanPackageMismatch(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report ComponentScan with descendant package`() {
        val code = """
            package com.example

            import org.koin.core.annotation.Module
            import org.koin.core.annotation.ComponentScan

            @Module
            @ComponentScan("com.example.feature")
            class RootModule
        """.trimIndent()

        val findings = ComponentScanPackageMismatch(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report ComponentScan without Module annotation`() {
        val code = """
            package com.example.feature

            import org.koin.core.annotation.ComponentScan

            @ComponentScan("com.other.module")
            class NotAModule
        """.trimIndent()

        val findings = ComponentScanPackageMismatch(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report ComponentScan without arguments`() {
        val code = """
            package com.example.feature

            import org.koin.core.annotation.Module
            import org.koin.core.annotation.ComponentScan

            @Module
            @ComponentScan
            class FeatureModule
        """.trimIndent()

        val findings = ComponentScanPackageMismatch(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }
}
