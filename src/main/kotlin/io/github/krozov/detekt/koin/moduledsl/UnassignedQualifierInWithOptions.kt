package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

internal class UnassignedQualifierInWithOptions(config: Config) : Rule(config) {

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
