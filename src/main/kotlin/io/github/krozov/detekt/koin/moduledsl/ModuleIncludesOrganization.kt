package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

internal class ModuleIncludesOrganization(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "ModuleIncludesOrganization",
        severity = Severity.Style,
        description = "Detects modules that mix many includes() with direct definitions. " +
                "Consider extracting definitions to separate modules.",
        debt = Debt.TEN_MINS
    )

    private val maxIncludesWithDefinitions: Int by config(3)

    override fun visitCallExpression(expression: KtCallExpression) {
        val callName = expression.getCallNameExpression()?.text
        if (callName != "module") {
            super.visitCallExpression(expression)
            return
        }

        // Get lambda body
        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()
            ?: expression.valueArguments.firstOrNull()?.getArgumentExpression() as? KtLambdaExpression
            ?: return

        val statements = lambda.bodyExpression?.statements ?: emptyList()

        var includesCount = 0
        var hasDefinitions = false

        statements.forEach { statement ->
            if (statement is KtCallExpression) {
                val stmtCallName = statement.getCallNameExpression()?.text
                when (stmtCallName) {
                    "includes" -> {
                        // Count arguments in includes()
                        includesCount += statement.valueArguments.size
                    }
                    in setOf("single", "factory", "scoped", "viewModel", "worker") -> {
                        hasDefinitions = true
                    }
                }
            }
        }

        if (hasDefinitions && includesCount > maxIncludesWithDefinitions) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    Module has $includesCount includes() and direct definitions (God Module pattern)
                    → Harder to understand, test, and maintain
                    → Extract definitions to a separate module

                    ✗ Bad:  module { includes(a, b, c, d); single { X() }; factory { Y() } }
                    ✓ Good: module { includes(a, b, c, d, localModule) }
                    """.trimIndent()
                )
            )
        }

        super.visitCallExpression(expression)
    }
}
