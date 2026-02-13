package io.github.krozov.detekt.koin.architecture

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlatformImportRestrictionTest {

    @Test
    fun `reports Android Koin import in non-Android package`() {
        val config = TestConfig(
            "restrictions" to listOf(
                mapOf(
                    "import" to "org.koin.android.*",
                    "allowedPackages" to listOf("com.example.app")
                )
            )
        )

        val code = """
            package com.example.shared

            import org.koin.android.ext.koin.androidContext

            fun setup() { }
        """.trimIndent()

        val findings = PlatformImportRestriction(config).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `allows Android Koin import in allowed package`() {
        val config = TestConfig(
            "restrictions" to listOf(
                mapOf(
                    "import" to "org.koin.android.*",
                    "allowedPackages" to listOf("com.example.app")
                )
            )
        )

        val code = """
            package com.example.app

            import org.koin.android.ext.koin.androidContext

            fun setup() { }
        """.trimIndent()

        val findings = PlatformImportRestriction(config).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does nothing when restrictions not configured`() {
        val code = """
            package com.example.shared

            import org.koin.android.ext.koin.androidContext
        """.trimIndent()

        val findings = PlatformImportRestriction(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows subpackages of allowed package`() {
        val config = TestConfig(
            "restrictions" to listOf(
                mapOf(
                    "import" to "org.koin.android.*",
                    "allowedPackages" to listOf("com.example.app")
                )
            )
        )

        val code = """
            package com.example.app.ui.main

            import org.koin.android.ext.koin.androidContext

            fun setup() { }
        """.trimIndent()

        val findings = PlatformImportRestriction(config).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `supports multiple restrictions`() {
        val config = TestConfig(
            "restrictions" to listOf(
                mapOf(
                    "import" to "org.koin.android.*",
                    "allowedPackages" to listOf("com.example.app")
                ),
                mapOf(
                    "import" to "org.koin.compose.*",
                    "allowedPackages" to listOf("com.example.ui")
                )
            )
        )

        val code = """
            package com.example.shared

            import org.koin.compose.koinInject

            fun setup() { }
        """.trimIndent()

        val findings = PlatformImportRestriction(config).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `supports exact import pattern without wildcard`() {
        val config = TestConfig(
            "restrictions" to listOf(
                mapOf(
                    "import" to "org.koin.android.ext.koin.androidContext",
                    "allowedPackages" to listOf("com.example.app")
                )
            )
        )

        val code = """
            package com.example.shared

            import org.koin.android.ext.koin.androidContext

            fun setup() { }
        """.trimIndent()

        val findings = PlatformImportRestriction(config).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows non-restricted import in any package`() {
        val config = TestConfig(
            "restrictions" to listOf(
                mapOf(
                    "import" to "org.koin.android.*",
                    "allowedPackages" to listOf("com.example.app")
                )
            )
        )

        val code = """
            package com.example.shared

            import org.koin.core.component.inject

            fun setup() { }
        """.trimIndent()

        val findings = PlatformImportRestriction(config).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `supports multiple allowed packages for single restriction`() {
        val config = TestConfig(
            "restrictions" to listOf(
                mapOf(
                    "import" to "org.koin.android.*",
                    "allowedPackages" to listOf("com.example.app", "com.example.mobile")
                )
            )
        )

        val codeApp = """
            package com.example.app

            import org.koin.android.ext.koin.androidContext

            fun setup() { }
        """.trimIndent()

        val codeMobile = """
            package com.example.mobile

            import org.koin.android.ext.koin.androidContext

            fun setup() { }
        """.trimIndent()

        assertThat(PlatformImportRestriction(config).lint(codeApp)).isEmpty()
        assertThat(PlatformImportRestriction(config).lint(codeMobile)).isEmpty()
    }
}
