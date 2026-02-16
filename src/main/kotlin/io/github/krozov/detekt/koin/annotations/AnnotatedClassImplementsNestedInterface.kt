package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass

/**
 * Detects Koin-annotated classes that implement nested/inner interfaces.
 *
 * KSP code generator drops the parent qualifier in `bind()` call, generating
 * `bind(ChildInterface::class)` instead of `bind(Parent.ChildInterface::class)`.
 *
 * <noncompliant>
 * @Single
 * class MyImpl : Parent.ChildInterface // KSP generates bind(ChildInterface::class) — wrong!
 * </noncompliant>
 *
 * <compliant>
 * // Extract to top-level:
 * interface ChildInterface
 *
 * @Single
 * class MyImpl : ChildInterface
 * </compliant>
 */
public class AnnotatedClassImplementsNestedInterface(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "AnnotatedClassImplementsNestedInterface",
        severity = Severity.Warning,
        description = "Koin-annotated class implements nested interface — KSP may drop parent qualifier",
        debt = Debt.TEN_MINS
    )

    private val koinDefinitionAnnotations = KoinAnnotationConstants.DEFINITION_ANNOTATIONS

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val annotations = klass.annotationEntries.mapNotNull { it.shortName?.asString() }
        if (annotations.none { it in koinDefinitionAnnotations }) return

        klass.superTypeListEntries.forEach { entry ->
            val typeText = entry.typeReference?.text ?: return@forEach
            val typeWithoutGenerics = typeText.substringBefore("<")
            // Nested interface reference contains a dot: Parent.ChildInterface
            // Skip fully-qualified names (e.g., com.example.MyInterface) where
            // the first segment starts with a lowercase letter (package convention).
            if ("." in typeWithoutGenerics && typeWithoutGenerics[0].isUpperCase()) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(entry),
                        """
                        Implements nested interface '$typeText' — KSP may drop parent qualifier in bind()
                        → Extract interface to top-level declaration to avoid resolution failure

                        ✗ Bad:  @Single class ${klass.name} : $typeText
                        ✓ Good: Extract ${typeText.substringAfterLast(".")} to top-level, then @Single class ${klass.name} : ${typeText.substringAfterLast(".")}
                        """.trimIndent()
                    )
                )
            }
        }
    }
}
