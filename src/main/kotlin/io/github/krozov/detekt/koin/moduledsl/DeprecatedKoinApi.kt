package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

internal class DeprecatedKoinApi(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "DeprecatedKoinApi",
        severity = Severity.Warning,
        description = "Detects usage of APIs deprecated in Koin 4.x with suggested replacements.",
        debt = Debt.FIVE_MINS
    )

    private val deprecations = mapOf(
        "checkModules" to "verify()",
        "koinNavViewModel" to "koinViewModel()",
        "stateViewModel" to "viewModel()",
        "viewModel" to "viewModelOf()",
        "getViewModel" to "get()"
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.getCallNameExpression()?.text ?: return
        val replacement = deprecations[callName]

        if (replacement != null) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    Deprecated API '$callName' used → May be removed in future Koin versions
                    → Migrate to '$replacement'

                    ✗ Bad:  $callName()
                    ✓ Good: $replacement
                    """.trimIndent()
                )
            )
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        val selectorText = expression.selectorExpression?.text
        if (selectorText == "koin") {
            val receiverName = (expression.receiverExpression as? KtNameReferenceExpression)?.text
            if (receiverName == "application") {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(expression),
                        """
                        Deprecated API 'application.koin' used → May be removed in future Koin versions
                        → Migrate to 'application.koinModules()'

                        ✗ Bad:  val koin = application.koin
                        ✓ Good: val modules = application.koinModules()
                        """.trimIndent()
                    )
                )
            }
        }
    }
}
