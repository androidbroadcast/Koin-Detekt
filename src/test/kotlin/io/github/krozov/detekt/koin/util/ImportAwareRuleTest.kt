package io.github.krozov.detekt.koin.util

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.github.detekt.test.utils.compileContentForTest
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.psi.KtClass
import org.junit.jupiter.api.Test

class ImportAwareRuleTest {

    /** Minimal rule that records the resolution of @Single for every class it visits. */
    private class TestRule : ImportAwareRule(Config.empty) {
        override val issue = Issue("Test", Severity.Warning, "test", Debt.FIVE_MINS)
        val resolutions = mutableListOf<Resolution>()

        override fun visitClass(klass: KtClass) {
            super.visitClass(klass)
            resolutions += importContext.resolveKoin("Single")
        }
    }

    @Test
    fun `importContext is EMPTY before visitKtFile`() {
        val rule = TestRule()
        assertThat(rule.importContext).isSameAs(FileImportContext.EMPTY)
    }

    @Test
    fun `importContext resolves correctly after file is visited`() {
        val rule = TestRule()
        rule.lint("""
            package com.example
            import org.koin.core.annotation.Single
            class Foo
        """.trimIndent())
        assertThat(rule.resolutions).containsExactly(Resolution.KOIN)
    }

    @Test
    fun `importContext updates per file`() {
        val rule = TestRule()
        // First file — koin import
        rule.visitKtFile(compileContentForTest("""
            package com.example
            import org.koin.core.annotation.Single
            class Foo
        """.trimIndent()))
        val firstResolution = rule.importContext.resolveKoin("Single")
        assertThat(firstResolution).isEqualTo(Resolution.KOIN)

        // Second file — non-koin import
        rule.visitKtFile(compileContentForTest("""
            package com.example
            import javax.inject.Single
            class Bar
        """.trimIndent()))
        val secondResolution = rule.importContext.resolveKoin("Single")
        assertThat(secondResolution).isEqualTo(Resolution.NOT_KOIN)
    }

    @Test
    fun `subclass can override visitKtFile and still get updated context`() {
        val resolutionsBeforeSuper = mutableListOf<Resolution>()

        val rule = object : ImportAwareRule(Config.empty) {
            override val issue = Issue("Test2", Severity.Warning, "test", Debt.FIVE_MINS)

            override fun visitKtFile(file: org.jetbrains.kotlin.psi.KtFile) {
                // context NOT yet updated here — super updates it
                resolutionsBeforeSuper += importContext.resolveKoin("Single")
                super.visitKtFile(file)
                // context IS updated after super
            }
        }

        rule.lint("""
            package com.example
            import org.koin.core.annotation.Single
            class Foo
        """.trimIndent())

        // Before super — still EMPTY sentinel → UNKNOWN
        assertThat(resolutionsBeforeSuper).containsExactly(Resolution.UNKNOWN)
    }
}
