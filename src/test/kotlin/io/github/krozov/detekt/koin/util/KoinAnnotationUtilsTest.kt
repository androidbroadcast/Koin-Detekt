package io.github.krozov.detekt.koin.util

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.psi.KtClass
import org.junit.jupiter.api.Test

class KoinAnnotationUtilsTest {

    private val ALLOWED = setOf("Single", "Factory")

    /**
     * Minimal rule that records results of [firstKoinAnnotationName] for every class it visits.
     */
    private inner class FirstNameRule : ImportAwareRule(Config.empty) {
        override val issue = Issue("TestFirst", Severity.Warning, "test", Debt.FIVE_MINS)
        val results = mutableListOf<String?>()

        override fun visitClass(klass: KtClass) {
            super.visitClass(klass)
            results += klass.firstKoinAnnotationName(importContext, ALLOWED)
        }
    }

    /**
     * Minimal rule that records results of [hasKoinAnnotationFrom] for every class it visits.
     */
    private inner class HasAnnotationRule : ImportAwareRule(Config.empty) {
        override val issue = Issue("TestHas", Severity.Warning, "test", Debt.FIVE_MINS)
        val results = mutableListOf<Boolean>()

        override fun visitClass(klass: KtClass) {
            super.visitClass(klass)
            results += klass.hasKoinAnnotationFrom(importContext, ALLOWED)
        }
    }

    /**
     * Minimal rule that records results of [koinAnnotationNames] for every class it visits.
     */
    private inner class AllNamesRule : ImportAwareRule(Config.empty) {
        override val issue = Issue("TestAll", Severity.Warning, "test", Debt.FIVE_MINS)
        val results = mutableListOf<List<String>>()

        override fun visitClass(klass: KtClass) {
            super.visitClass(klass)
            results += klass.koinAnnotationNames(importContext, ALLOWED)
        }
    }

    // ── firstKoinAnnotationName ────────────────────────────────────────────

    @Test
    fun `firstKoinAnnotationName returns name when Koin import is present`() {
        val rule = FirstNameRule()
        rule.lint("""
            package com.example
            import org.koin.core.annotation.Single
            @Single
            class Foo
        """.trimIndent())
        assertThat(rule.results).containsExactly("Single")
    }

    @Test
    fun `firstKoinAnnotationName returns null when non-Koin import is present`() {
        val rule = FirstNameRule()
        rule.lint("""
            package com.example
            import com.other.Single
            @Single
            class Foo
        """.trimIndent())
        assertThat(rule.results).containsExactly(null)
    }

    @Test
    fun `firstKoinAnnotationName returns name for UNKNOWN resolution (no import)`() {
        val rule = FirstNameRule()
        // No import at all — resolution is UNKNOWN, treated as potentially Koin
        rule.lint("""
            package com.example
            @Single
            class Foo
        """.trimIndent())
        assertThat(rule.results).containsExactly("Single")
    }

    @Test
    fun `firstKoinAnnotationName returns null when annotation is not in allowed set`() {
        val rule = FirstNameRule()
        rule.lint("""
            package com.example
            import org.koin.core.annotation.Scoped
            @Scoped
            class Foo
        """.trimIndent())
        assertThat(rule.results).containsExactly(null)
    }

    @Test
    fun `firstKoinAnnotationName returns first matching name when multiple present`() {
        val rule = FirstNameRule()
        rule.lint("""
            package com.example
            import org.koin.core.annotation.Single
            import org.koin.core.annotation.Factory
            @Single @Factory
            class Foo
        """.trimIndent())
        // Single appears first in annotation list
        assertThat(rule.results).containsExactly("Single")
    }

    // ── hasKoinAnnotationFrom ──────────────────────────────────────────────

    @Test
    fun `hasKoinAnnotationFrom returns true when Koin import is present`() {
        val rule = HasAnnotationRule()
        rule.lint("""
            package com.example
            import org.koin.core.annotation.Factory
            @Factory
            class Foo
        """.trimIndent())
        assertThat(rule.results).containsExactly(true)
    }

    @Test
    fun `hasKoinAnnotationFrom returns false when non-Koin import is present`() {
        val rule = HasAnnotationRule()
        rule.lint("""
            package com.example
            import com.other.Single
            @Single
            class Foo
        """.trimIndent())
        assertThat(rule.results).containsExactly(false)
    }

    @Test
    fun `hasKoinAnnotationFrom returns false when class has no annotation from allowed set`() {
        val rule = HasAnnotationRule()
        rule.lint("""
            package com.example
            import org.koin.core.annotation.Scoped
            @Scoped
            class Foo
        """.trimIndent())
        assertThat(rule.results).containsExactly(false)
    }

    // ── koinAnnotationNames ────────────────────────────────────────────────

    @Test
    fun `koinAnnotationNames returns all KOIN and UNKNOWN annotations, filters NOT_KOIN`() {
        val rule = AllNamesRule()
        // Single → Koin import (KOIN), Factory → no import (UNKNOWN), Scoped → not in allowed set
        rule.lint("""
            package com.example
            import org.koin.core.annotation.Single
            @Single @Factory
            class Foo
        """.trimIndent())
        // Both Single (KOIN) and Factory (UNKNOWN) pass the filter; order matches annotation order
        assertThat(rule.results).hasSize(1)
        assertThat(rule.results[0]).containsExactly("Single", "Factory")
    }

    @Test
    fun `koinAnnotationNames excludes NOT_KOIN annotations`() {
        val rule = AllNamesRule()
        // Single → non-Koin import → NOT_KOIN; Factory → no import → UNKNOWN
        rule.lint("""
            package com.example
            import com.other.Single
            @Single @Factory
            class Foo
        """.trimIndent())
        assertThat(rule.results).hasSize(1)
        // Single is excluded (NOT_KOIN), Factory survives (UNKNOWN)
        assertThat(rule.results[0]).containsExactly("Factory")
    }

    @Test
    fun `koinAnnotationNames returns empty list when all annotations are NOT_KOIN`() {
        val rule = AllNamesRule()
        rule.lint("""
            package com.example
            import com.other.Single
            import com.other.Factory
            @Single @Factory
            class Foo
        """.trimIndent())
        assertThat(rule.results).hasSize(1)
        assertThat(rule.results[0]).isEmpty()
    }

    @Test
    fun `koinAnnotationNames returns empty list when no annotations from allowed set`() {
        val rule = AllNamesRule()
        rule.lint("""
            package com.example
            class Foo
        """.trimIndent())
        assertThat(rule.results).hasSize(1)
        assertThat(rule.results[0]).isEmpty()
    }
}
