package io.github.krozov.detekt.koin.moduledsl

import io.github.krozov.detekt.koin.util.ImportAwareRule
import io.github.krozov.detekt.koin.util.Resolution
import io.github.krozov.detekt.koin.util.resolveKoin
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

internal class UnassignedQualifierInWithOptions(config: Config) : ImportAwareRule(config) {

    override val issue = Issue(
        id = "UnassignedQualifierInWithOptions",
        severity = Severity.Warning,
        description = "Detects named() calls in withOptions {} without assignment to qualifier property",
        debt = Debt.FIVE_MINS
    )

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        super.visitBinaryExpression(expression)

        // Find "withOptions" infix calls
        if (expression.operationReference.text != "withOptions") return
        if (importContext.resolveKoin("withOptions") == Resolution.NOT_KOIN) return

        // Get the lambda argument (right side of infix)
        val rightSide = expression.right as? KtLambdaExpression ?: return
        val bodyExpression = rightSide.bodyExpression ?: return

        // Look for named() or qualifier() calls that are not assigned
        val statements = bodyExpression.statements
        for (statement in statements) {
            if (statement is KtCallExpression) {
                val callName = statement.getCallNameExpression()?.text
                if (callName == "named" || callName == "qualifier") {
                    // This is an unassigned call
                    report(
                        CodeSmell(
                            issue,
                            Entity.from(expression),
                            """
                            Unassigned qualifier in withOptions → Dead code, qualifier not applied
                            → Assign to qualifier property

                            ✗ Bad:  withOptions { named("x") }
                            ✓ Good: withOptions { qualifier = named("x") }
                            """.trimIndent()
                        )
                    )
                    return
                }
            }
        }
    }
}
