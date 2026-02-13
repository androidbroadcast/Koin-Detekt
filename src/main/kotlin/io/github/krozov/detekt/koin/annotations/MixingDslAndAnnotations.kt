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

/**
 * Detects mixing DSL (`module {}`) and Annotations (`@Module`, `@Single`) in same file.
 *
 * Mixing both approaches in the same file is inconsistent and harder to maintain.
 * Choose one approach per file.
 *
 * <noncompliant>
 * @Module
 * class AnnotatedModule {
 *     @Single
 *     fun provideRepo(): Repository = ...
 * }
 *
 * val dslModule = module {
 *     single { ApiService() }
 * }
 * </noncompliant>
 *
 * <compliant>
 * // Either all DSL:
 * val module = module {
 *     single { Repository() }
 *     single { ApiService() }
 * }
 *
 * // Or all Annotations:
 * @Module
 * class MyModule {
 *     @Single
 *     fun provideRepo(): Repository = ...
 *     @Single
 *     fun provideApi(): ApiService = ...
 * }
 * </compliant>
 */
public class MixingDslAndAnnotations(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "MixingDslAndAnnotations",
        severity = Severity.Warning,
        description = "Mixing DSL and Annotations in same file is inconsistent",
        debt = Debt.FIVE_MINS
    )

    private var hasModuleCall = false
    private var hasModuleAnnotation = false
    private var moduleCallElement: KtCallExpression? = null

    override fun visitKtFile(file: KtFile) {
        hasModuleCall = false
        hasModuleAnnotation = false
        moduleCallElement = null

        // Check for Koin annotations
        file.declarations.forEach { declaration ->
            val annotations = declaration.annotationEntries.mapNotNull { it.shortName?.asString() }
            if ("Module" in annotations || "Single" in annotations || "Factory" in annotations) {
                hasModuleAnnotation = true
            }
        }

        super.visitKtFile(file)

        // Report if mixing both
        if (hasModuleCall && hasModuleAnnotation && moduleCallElement != null) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(moduleCallElement!!),
                    """
                    Mixing DSL and Annotations in same file → Inconsistent, harder to maintain
                    → Choose one approach per file for consistency

                    ✗ Bad:  @Module class MyModule; val dslModule = module { }
                    ✓ Good: val module = module { single { Repo() }; single { Api() } }
                    """.trimIndent()
                )
            )
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.calleeExpression?.text ?: return
        if (callName == "module") {
            hasModuleCall = true
            if (moduleCallElement == null) {
                moduleCallElement = expression
            }
        }
    }
}
