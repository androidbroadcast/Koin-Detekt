package io.github.krozov.detekt.koin.annotations

import io.github.krozov.detekt.koin.util.ImportAwareRule
import io.github.krozov.detekt.koin.util.koinAnnotationNames
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass

/**
 * A class has multiple Koin definition annotations (e.g., both `@Single` and `@Factory`).
 *
 * KSP picks the first annotation it encounters; the remaining annotations are silently
 * ignored, making the lifecycle behavior undefined and surprising.
 *
 * <noncompliant>
 * @Single
 * @Factory
 * class MyService
 * </noncompliant>
 *
 * <compliant>
 * @Single
 * class MyService
 * </compliant>
 */
internal class MultipleKoinDefinitionAnnotations(config: Config = Config.empty) : ImportAwareRule(config) {
    override val issue: Issue = Issue(
        id = "MultipleKoinDefinitionAnnotations",
        severity = Severity.Warning,
        description = "Class has multiple Koin definition annotations; KSP picks first and ignores the rest",
        debt = Debt.FIVE_MINS
    )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val koinAnnotations = klass.koinAnnotationNames(importContext, KoinAnnotationConstants.DEFINITION_ANNOTATIONS)

        if (koinAnnotations.size > 1) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(klass),
                    """
                    Multiple Koin definition annotations: ${koinAnnotations.joinToString(", ") { "@$it" }}
                    → KSP picks first annotation; behavior is undefined. Choose one.

                    ✗ Bad:  ${koinAnnotations.joinToString(" ") { "@$it" }} class ${klass.name}
                    ✓ Good: @${koinAnnotations.first()} class ${klass.name}
                    """.trimIndent()
                )
            )
        }
    }
}
