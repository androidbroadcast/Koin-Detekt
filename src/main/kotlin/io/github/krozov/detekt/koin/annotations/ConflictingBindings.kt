package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Same type defined in both DSL and Annotations.
 *
 * Runtime conflict - which definition wins?
 *
 * <noncompliant>
 * @Module
 * class AnnotatedModule {
 *     @Single
 *     fun provideRepo(): Repository = RepoImpl()
 * }
 *
 * val dslModule = module {
 *     single<Repository> { RepoImpl() } // Conflict!
 * }
 * </noncompliant>
 *
 * <compliant>
 * // Choose one approach:
 * @Module
 * class AnnotatedModule {
 *     @Single
 *     fun provideRepo(): Repository = RepoImpl()
 * }
 * </compliant>
 */
public class ConflictingBindings(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "ConflictingBindings",
        severity = Severity.Warning,
        description = "Same type defined in both DSL and Annotations",
        debt = Debt.TEN_MINS
    )

    private val annotatedTypes = mutableSetOf<String>()
    private val dslTypes = mutableSetOf<String>()
    private val typeToElement = mutableMapOf<String, Any>()

    override fun visitKtFile(file: KtFile) {
        annotatedTypes.clear()
        dslTypes.clear()
        typeToElement.clear()

        super.visitKtFile(file)

        // Find conflicts
        val conflicts = annotatedTypes.intersect(dslTypes)
        conflicts.forEach { type ->
            typeToElement[type]?.let { element ->
                report(
                    CodeSmell(
                        issue,
                        Entity.from(element as org.jetbrains.kotlin.psi.KtElement),
                        "Type '$type' defined in both DSL and Annotations. " +
                                "This creates a runtime conflict. Use only one approach."
                    )
                )
            }
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        val annotations = function.annotationEntries.mapNotNull { it.shortName?.asString() }
        if (annotations.any { it in setOf("Single", "Factory", "Scoped") }) {
            val returnType = function.typeReference?.text
            if (returnType != null) {
                val typeName = returnType.substringBefore("<").substringAfterLast(".")
                annotatedTypes.add(typeName)
                if (typeName !in typeToElement) {
                    typeToElement[typeName] = function
                }
            }
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.calleeExpression?.text ?: return
        if (callName in setOf("single", "factory", "scoped")) {
            // Try to extract type from type argument
            val typeArgs = expression.typeArgumentList?.arguments
            val typeName = typeArgs?.firstOrNull()?.typeReference?.text
                ?.substringBefore("<")
                ?.substringAfterLast(".")

            if (typeName != null) {
                dslTypes.add(typeName)
                if (typeName !in typeToElement) {
                    typeToElement[typeName] = expression
                }
            }
        }
    }
}
