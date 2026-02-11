package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

internal class EmptyModule(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "EmptyModule",
        severity = Severity.Warning,
        description = "Detects Koin modules without any definitions or includes(). " +
                "Empty modules should be removed.",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.getCallNameExpression()?.text
        if (callName != "module") return

        // Get the lambda argument
        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()
            ?: expression.valueArguments.firstOrNull()?.getArgumentExpression() as? KtLambdaExpression
            ?: return

        val bodyExpression = lambda.bodyExpression
        val statements = bodyExpression?.statements ?: emptyList()

        if (statements.isEmpty()) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Module is empty. Remove it or add definitions/includes()."
                )
            )
        }
    }
}
