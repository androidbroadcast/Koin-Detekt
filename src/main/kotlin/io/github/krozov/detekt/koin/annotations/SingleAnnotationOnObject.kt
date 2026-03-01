package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.github.krozov.detekt.koin.util.ImportAwareRule
import io.github.krozov.detekt.koin.util.firstKoinAnnotationName
import org.jetbrains.kotlin.psi.KtObjectDeclaration

/**
 * Detects Koin definition annotations (`@Single`, `@Factory`, etc.) on Kotlin `object` declarations.
 *
 * Objects are language-level singletons. Annotating them with `@Single` generates invalid code
 * like `single { MyObject() }` — calling a non-existent constructor.
 *
 * <noncompliant>
 * @Single
 * object MySingleton // Generates invalid: single { MySingleton() }
 * </noncompliant>
 *
 * <compliant>
 * @Single
 * class MySingleton // Correct: single { MySingleton() }
 * </compliant>
 */
internal class SingleAnnotationOnObject(config: Config = Config.empty) : ImportAwareRule(config) {
    override val issue: Issue = Issue(
        id = "SingleAnnotationOnObject",
        severity = Severity.Warning,
        description = "Koin annotation on object declaration generates invalid constructor call",
        debt = Debt.FIVE_MINS
    )

    private val koinDefinitionAnnotations = KoinAnnotationConstants.DEFINITION_ANNOTATIONS

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
        super.visitObjectDeclaration(declaration)

        if (declaration.isCompanion()) return

        val koinAnnotation = declaration.firstKoinAnnotationName(importContext, koinDefinitionAnnotations) ?: return

        report(
            CodeSmell(
                issue,
                Entity.from(declaration),
                """
                @$koinAnnotation on object declaration generates invalid constructor call
                → Objects are already singletons — remove the annotation or convert to a class

                ✗ Bad:  @$koinAnnotation object ${declaration.name}
                ✓ Good: @$koinAnnotation class ${declaration.name}
                """.trimIndent()
            )
        )
    }
}
