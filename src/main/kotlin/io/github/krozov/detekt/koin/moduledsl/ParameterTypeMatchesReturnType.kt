package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.*

internal class ParameterTypeMatchesReturnType(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "ParameterTypeMatchesReturnType",
        severity = Severity.Warning,
        description = "Detects factory/single/scoped definitions where return type matches parameter type, " +
                "causing parametersOf() to return the parameter directly instead of executing the definition",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val calleeName = expression.calleeExpression?.text ?: return
        if (calleeName !in setOf("factory", "single", "scoped")) return

        // Get type argument: factory<Int>
        val typeArgument = expression.typeArguments.firstOrNull()?.text ?: return

        // Get lambda parameter type
        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression() ?: return
        val paramType = lambda.valueParameters.firstOrNull()?.typeReference?.text ?: return

        // Normalize (remove nullability)
        val normalizedReturn = typeArgument.removeSuffix("?")
        val normalizedParam = paramType.removeSuffix("?")

        if (normalizedReturn == normalizedParam) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    Definition return type matches parameter type → parametersOf short-circuits
                    → Koin returns parameter directly without executing $calleeName

                    ✗ Bad:  $calleeName<$typeArgument> { limit: $paramType -> ... }
                    ✓ Good: Use different parameter type or remove type argument
                    """.trimIndent()
                )
            )
        }
    }
}
