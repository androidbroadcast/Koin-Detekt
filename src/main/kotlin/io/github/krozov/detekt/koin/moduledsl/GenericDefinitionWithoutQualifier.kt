package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.*

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

        if (!hasQualifier) {
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
