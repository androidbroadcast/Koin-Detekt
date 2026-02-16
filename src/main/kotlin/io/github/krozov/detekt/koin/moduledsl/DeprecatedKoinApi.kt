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
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
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
            // Skip viewModel -> viewModelOf() suggestion when lambda has named dependencies
            if (callName == "viewModel" && hasNamedDependenciesInLambda(expression)) {
                return
            }

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

    private val qualifierCallNames: Set<String> = setOf("named", "qualifier", "StringQualifier")

    private fun hasNamedDependenciesInLambda(expression: KtCallExpression): Boolean {
        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()
            ?: expression.valueArguments
                .mapNotNull { it.getArgumentExpression() as? KtLambdaExpression }
                .firstOrNull()
            ?: return false
        return containsQualifierCall(lambda)
    }

    private fun containsQualifierCall(element: KtElement): Boolean {
        if (element is KtCallExpression) {
            val name = element.getCallNameExpression()?.text
            if (name in qualifierCallNames) return true
        }
        return element.children.any { child ->
            (child as? KtElement)?.let { containsQualifierCall(it) } == true
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
