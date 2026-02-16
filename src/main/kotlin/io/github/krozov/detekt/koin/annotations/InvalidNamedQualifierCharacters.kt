package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Detects `@Named` values containing characters invalid in generated Kotlin identifiers.
 *
 * Hyphens, spaces, and special characters in `@Named` values cause KSP code generation failures.
 *
 * <noncompliant>
 * @Named("ricky-morty")
 * @Single
 * class MyService
 * </noncompliant>
 *
 * <compliant>
 * @Named("rickyMorty")
 * @Single
 * class MyService
 * </compliant>
 */
public class InvalidNamedQualifierCharacters(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "InvalidNamedQualifierCharacters",
        severity = Severity.Warning,
        description = "@Named value contains characters invalid in generated Kotlin identifiers",
        debt = Debt.FIVE_MINS
    )

    private val validPattern = Regex("^[a-zA-Z][a-zA-Z0-9_.]*$")

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
        super.visitAnnotationEntry(annotationEntry)

        if (annotationEntry.shortName?.asString() != "Named") return

        val args = annotationEntry.valueArgumentList?.arguments ?: return
        val firstArg = args.firstOrNull() ?: return

        val stringExpr = firstArg.getArgumentExpression() as? KtStringTemplateExpression ?: return
        val namedValue = stringExpr.entries.joinToString("") { it.text }

        if (namedValue.isNotEmpty() && !validPattern.matches(namedValue)) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(annotationEntry),
                    """
                    @Named("$namedValue") contains invalid characters for generated Kotlin identifiers
                    → Use only letters, digits, underscores, and dots

                    ✗ Bad:  @Named("$namedValue")
                    ✓ Good: @Named("${namedValue.replace(Regex("[^a-zA-Z0-9_.]"), "_")}")
                    """.trimIndent()
                )
            )
        }
    }
}
