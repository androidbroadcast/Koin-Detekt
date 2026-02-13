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
 * `@Scoped` without scope name/qualifier.
 *
 * Default scope may be unclear, better to be explicit.
 *
 * <noncompliant>
 * @Scoped
 * class MyService // Which scope?
 * </noncompliant>
 *
 * <compliant>
 * @Scoped(name = "userScope")
 * class MyService
 * </compliant>
 */
public class ScopedWithoutQualifier(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "ScopedWithoutQualifier",
        severity = Severity.Warning,
        description = "@Scoped should specify scope name for clarity",
        debt = Debt.FIVE_MINS
    )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val scopedAnnotation = klass.annotationEntries.find {
            it.shortName?.asString() == "Scoped"
        }

        if (scopedAnnotation != null) {
            // Check if annotation has parameters
            val hasParameters = scopedAnnotation.valueArgumentList != null &&
                    scopedAnnotation.valueArgumentList!!.arguments.isNotEmpty()

            if (!hasParameters) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(scopedAnnotation),
                        "@Scoped annotation should specify scope qualifier (e.g., @Scoped(name = \"userScope\")). " +
                                "Default scope may be unclear."
                    )
                )
            }
        }
    }
}
