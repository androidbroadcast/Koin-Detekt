package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression

internal class GenericDefinitionWithoutQualifier(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "GenericDefinitionWithoutQualifier",
        severity = Severity.Warning,
        description = "Generic types without qualifiers cause type erasure collisions",
        debt = Debt.TEN_MINS
    )

    // Detect both type names and factory functions for generics
    private val genericPatterns = listOf(
        Regex("""List\s*<"""),
        Regex("""listOf\s*<"""),
        Regex("""Set\s*<"""),
        Regex("""setOf\s*<"""),
        Regex("""Map\s*<"""),
        Regex("""mapOf\s*<"""),
        Regex("""Array\s*<"""),
        Regex("""arrayOf\s*<""")
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        // Check if this is a Koin definition call
        val calleeName = expression.calleeExpression?.text ?: return
        if (calleeName !in setOf("single", "factory", "scoped")) return

        // Get lambda body to check for generic types
        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()
        if (lambda == null) return

        val lambdaBodyText = lambda.bodyExpression?.text ?: return

        // Check if lambda contains generic type usage
        val hasGeneric = genericPatterns.any { pattern ->
            pattern.containsMatchIn(lambdaBodyText)
        }

        if (!hasGeneric) return

        // Check if this definition has a qualifier argument
        val hasQualifier = expression.valueArguments.any { arg ->
            val argText = arg.getArgumentExpression()?.text ?: ""
            argText.contains("named(") || argText.contains("qualifier(")
        }

        // Check for qualifier in chained withOptions — supports both call styles:
        // dot:    single { ... }.withOptions { qualifier = named(...) }
        // infix:  single { ... } withOptions { qualifier = named(...) }
        val hasQualifierInOptions = run {
            fun String.hasQualifier() = contains("named(") || contains("qualifier(")

            val dotExpr = expression.parent as? KtDotQualifiedExpression
            if (dotExpr != null && dotExpr.receiverExpression == expression) {
                val withOptionsCall = dotExpr.selectorExpression as? KtCallExpression ?: return@run false
                if (withOptionsCall.calleeExpression?.text != "withOptions") return@run false
                val body = withOptionsCall.lambdaArguments.firstOrNull()
                    ?.getLambdaExpression()?.bodyExpression?.text ?: return@run false
                return@run body.hasQualifier()
            }

            val binaryExpr = expression.parent as? KtBinaryExpression
            if (binaryExpr != null && binaryExpr.left == expression &&
                binaryExpr.operationReference.text == "withOptions"
            ) {
                val body = (binaryExpr.right as? KtLambdaExpression)
                    ?.bodyExpression?.text ?: return@run false
                return@run body.hasQualifier()
            }

            false
        }

        if (!hasQualifier && !hasQualifierInOptions) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    Generic type without qualifier → Type erasure collision
                    ✗ Bad:  single { listOf<A>() }
                    ✓ Good: single(named("a")) { listOf<A>() }
                    """.trimIndent()
                )
            )
        }
    }
}
