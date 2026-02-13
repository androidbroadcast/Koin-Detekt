package io.github.krozov.detekt.koin.architecture

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LayerBoundaryViolationTest {

    @Test
    fun `reports Koin import in domain layer`() {
        val config = TestConfig(
            "restrictedLayers" to listOf("com.example.domain")
        )

        val code = """
            package com.example.domain

            import org.koin.core.component.get

            class UseCase {
                val repo = get<Repository>()
            }
        """.trimIndent()

        val findings = LayerBoundaryViolation(config).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `allows Koin import in unrestricted layer`() {
        val config = TestConfig(
            "restrictedLayers" to listOf("com.example.domain")
        )

        val code = """
            package com.example.data

            import org.koin.core.component.get

            class RepositoryImpl {
                val api = get<ApiService>()
            }
        """.trimIndent()

        val findings = LayerBoundaryViolation(config).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows allowed imports in restricted layer`() {
        val config = TestConfig(
            "restrictedLayers" to listOf("com.example.domain"),
            "allowedImports" to listOf("org.koin.core.qualifier.Qualifier")
        )

        val code = """
            package com.example.domain

            import org.koin.core.qualifier.Qualifier

            class UseCase(qualifier: Qualifier)
        """.trimIndent()

        val findings = LayerBoundaryViolation(config).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does nothing when restrictedLayers not configured`() {
        val code = """
            package com.example.domain

            import org.koin.core.component.get

            class UseCase
        """.trimIndent()

        val findings = LayerBoundaryViolation(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports star import in restricted layer`() {
        val config = TestConfig(
            "restrictedLayers" to listOf("com.example.domain")
        )

        val code = """
            package com.example.domain

            import org.koin.core.component.*

            class UseCase
        """.trimIndent()

        val findings = LayerBoundaryViolation(config).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports multiple Koin imports in restricted layer`() {
        val config = TestConfig(
            "restrictedLayers" to listOf("com.example.domain")
        )

        val code = """
            package com.example.domain

            import org.koin.core.component.get
            import org.koin.core.component.inject

            class UseCase
        """.trimIndent()

        val findings = LayerBoundaryViolation(config).lint(code)
        assertThat(findings).hasSize(2)
    }

    @Test
    fun `allows non-Koin imports in restricted layer`() {
        val config = TestConfig(
            "restrictedLayers" to listOf("com.example.domain")
        )

        val code = """
            package com.example.domain

            import kotlin.collections.List
            import com.example.common.Result

            class UseCase
        """.trimIndent()

        val findings = LayerBoundaryViolation(config).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `matches package prefix for restricted layers`() {
        val config = TestConfig(
            "restrictedLayers" to listOf("com.example.domain")
        )

        val code = """
            package com.example.domain.user

            import org.koin.core.component.get

            class UserUseCase
        """.trimIndent()

        val findings = LayerBoundaryViolation(config).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows subpackage imports when allowed`() {
        val config = TestConfig(
            "restrictedLayers" to listOf("com.example.domain"),
            "allowedImports" to listOf("org.koin.core.qualifier.Qualifier")
        )

        val code = """
            package com.example.domain

            import org.koin.core.qualifier.Qualifier.StringQualifier

            class UseCase
        """.trimIndent()

        val findings = LayerBoundaryViolation(config).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles multiple restricted layers`() {
        val config = TestConfig(
            "restrictedLayers" to listOf("com.example.domain", "com.example.core")
        )

        val code1 = """
            package com.example.domain

            import org.koin.core.component.get

            class DomainUseCase
        """.trimIndent()

        val code2 = """
            package com.example.core

            import org.koin.core.component.get

            class CoreService
        """.trimIndent()

        val findings1 = LayerBoundaryViolation(config).lint(code1)
        val findings2 = LayerBoundaryViolation(config).lint(code2)

        assertThat(findings1).hasSize(1)
        assertThat(findings2).hasSize(1)
    }

    @Test
    fun `logs warning when restrictedLayers is empty`() {
        val config = TestConfig("restrictedLayers" to emptyList<String>())

        val code = """
            import org.koin.core.component.KoinComponent

            class DomainService : KoinComponent
        """.trimIndent()

        // Should not report violations when inactive
        val findings = LayerBoundaryViolation(config).lint(code)
        assertThat(findings).isEmpty()

        // TODO: Verify warning logged (would need log capture)
    }

    @Test
    fun `reports error for invalid config type`() {
        // This test documents expected behavior when config is wrong type
        // Detekt handles this at framework level, but we document it
    }
}
