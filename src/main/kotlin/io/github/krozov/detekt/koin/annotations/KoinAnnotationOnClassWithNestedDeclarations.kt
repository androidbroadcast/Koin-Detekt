package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration

/**
 * Detects Koin-annotated classes that contain nested class or object declarations (excluding companion objects).
 *
 * Nested declarations inside a Koin-annotated class confuse KSP code generation:
 * - KSP may attempt to generate bindings for nested types unexpectedly
 * - The generated factory may capture unintended state from the nested declarations
 * - Code structure is misleading — Koin components should be simple data providers
 *
 * Companion objects are exempt because they are static by nature and don't affect
 * the Koin lifecycle of the enclosing class.
 *
 * <noncompliant>
 * @Single
 * class MyService {
 *     class Config(val url: String)  // nested class — confuses KSP
 *     object Helper                  // nested object — confuses KSP
 * }
 * </noncompliant>
 *
 * <compliant>
 * @Single
 * class MyService {
 *     companion object { ... }  // companion is fine
 * }
 *
 * // Extract nested declarations to top level:
 * class MyServiceConfig(val url: String)
 *
 * @Single
 * class MyService
 * </compliant>
 */
public class KoinAnnotationOnClassWithNestedDeclarations(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "KoinAnnotationOnClassWithNestedDeclarations",
        severity = Severity.Warning,
        description = "Koin-annotated class contains nested class/object declarations — extract to top-level",
        debt = Debt.TEN_MINS
    )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val koinAnnotation = klass.annotationEntries
            .mapNotNull { it.shortName?.asString() }
            .firstOrNull { it in KoinAnnotationConstants.DEFINITION_ANNOTATIONS } ?: return

        val companionObjects: Set<KtDeclaration> = klass.companionObjects.toHashSet()
        val nestedDeclarations = klass.declarations.filter { decl ->
            decl is KtClass ||
                (decl is KtObjectDeclaration && decl !in companionObjects)
        }

        if (nestedDeclarations.isNotEmpty()) {
            val nestedNames = nestedDeclarations.mapNotNull {
                when (it) {
                    is KtClass -> it.name
                    is KtObjectDeclaration -> it.name
                    else -> null
                }
            }.joinToString(", ")

            report(
                CodeSmell(
                    issue,
                    Entity.from(klass),
                    """
                    @$koinAnnotation class '${klass.name}' contains nested declarations: $nestedNames
                    → Nested classes/objects inside Koin-annotated classes confuse KSP code generation
                    → Extract nested declarations to top-level or move to a separate file

                    ✗ Bad:  @$koinAnnotation class ${klass.name} { class $nestedNames(...) }
                    ✓ Good: class $nestedNames(...); @$koinAnnotation class ${klass.name}
                    """.trimIndent()
                )
            )
        }
    }
}
