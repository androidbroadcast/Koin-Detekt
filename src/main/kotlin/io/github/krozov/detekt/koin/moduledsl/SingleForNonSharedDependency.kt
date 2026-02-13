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

internal class SingleForNonSharedDependency(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "SingleForNonSharedDependency",
        severity = Severity.Warning,
        description = "Detects single/singleOf for types that should not be singletons by naming convention. " +
                "Use factory/factoryOf instead.",
        debt = Debt.TEN_MINS
    )

    private val namePatterns: List<String> by config(
        listOf(".*UseCase", ".*Interactor", ".*Mapper")
    )

    private val patterns by lazy {
        namePatterns.map { it.toRegex() }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.getCallNameExpression()?.text
        if (callName !in setOf("single", "singleOf")) return

        val typeName = extractTypeName(expression) ?: return

        val matchedPattern = patterns.firstOrNull { it.matches(typeName) }
        if (matchedPattern != null) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    '$typeName' registered as singleton (matches pattern '${matchedPattern.pattern}')
                    → Can cause memory leaks and stale state with per-request types
                    → Use factory/factoryOf for non-shared dependencies

                    ✗ Bad:  single { GetUserUseCase(get()) }
                    ✓ Good: factory { GetUserUseCase(get()) }
                    """.trimIndent()
                )
            )
        }
    }

    private fun extractTypeName(expression: KtCallExpression): String? {
        // For single { Type() }
        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()
            ?: expression.valueArguments.firstOrNull()?.getArgumentExpression() as? KtLambdaExpression

        if (lambda != null) {
            val bodyExpr = lambda.bodyExpression?.statements?.firstOrNull() as? KtCallExpression
            return bodyExpr?.getCallNameExpression()?.text
        }

        // For singleOf(::Type)
        val arg = expression.valueArguments.firstOrNull()?.getArgumentExpression()?.text
        return arg?.removePrefix("::")
    }
}
