package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.github.krozov.detekt.koin.util.ImportAwareRule
import io.github.krozov.detekt.koin.util.firstKoinAnnotationName
import org.jetbrains.kotlin.psi.KtClass

/**
 * Detects Koin definition annotations on classes that contain nested class declarations.
 *
 * When a Koin-annotated class has nested classes, it can cause confusion about which
 * class is being registered as a Koin component. The outer annotated class is registered,
 * while nested classes are ignored by KSP.
 *
 * <noncompliant>
 * @Single
 * class OuterService {
 *     class NestedHelper // This nested class is NOT registered by Koin
 * }
 * </noncompliant>
 *
 * <compliant>
 * @Single
 * class OuterService // No nested class declarations
 * </compliant>
 */
internal class KoinAnnotationOnClassWithNestedDeclarations(config: Config = Config.empty) : ImportAwareRule(config) {
    override val issue: Issue = Issue(
        id = "KoinAnnotationOnClassWithNestedDeclarations",
        severity = Severity.Warning,
        description = "Koin annotation on class with nested declarations — nested classes are ignored by KSP",
        debt = Debt.FIVE_MINS
    )

    private val koinDefinitionAnnotations = KoinAnnotationConstants.DEFINITION_ANNOTATIONS

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val koinAnnotation = klass.firstKoinAnnotationName(importContext, koinDefinitionAnnotations) ?: return

        val nestedClasses = klass.declarations.filterIsInstance<KtClass>()
        if (nestedClasses.isEmpty()) return

        report(
            CodeSmell(
                issue,
                Entity.from(klass),
                """
                @$koinAnnotation on class ${klass.name} which has nested class declarations
                → Nested classes are ignored by KSP — move nested classes out or use companion objects

                ✗ Bad:  @$koinAnnotation class ${klass.name} { class Nested }
                ✓ Good: @$koinAnnotation class ${klass.name}
                """.trimIndent()
            )
        )
    }
}
