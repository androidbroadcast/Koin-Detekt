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

internal class MissingScopedDependencyQualifier(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "MissingScopedDependencyQualifier",
        severity = Severity.Warning,
        description = "Detects multiple definitions of the same type in one module without named() qualifier. " +
                "This leads to runtime DefinitionOverrideException.",
        debt = Debt.TEN_MINS
    )

    private val allowOneDefault: Boolean by config(true)

    private val definitionsByModule = mutableMapOf<KtCallExpression, MutableList<TypeDefinition>>()

    internal data class TypeDefinition(
        val type: String,
        val hasQualifier: Boolean,
        val expression: KtCallExpression
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        val callName = expression.getCallNameExpression()?.text

        // Track module definitions
        if (callName == "module") {
            definitionsByModule[expression] = mutableListOf()
            super.visitCallExpression(expression)
            checkForDuplicates(expression)
            return
        }

        // Track definitions inside module
        if (callName in setOf("single", "factory", "scoped", "viewModel", "worker")) {
            val moduleCall = findParentModule(expression)
            if (moduleCall != null) {
                val typeName = extractTypeName(expression)
                val hasQualifier = hasQualifierArgument(expression)

                if (typeName != null) {
                    definitionsByModule[moduleCall]?.add(
                        TypeDefinition(typeName, hasQualifier, expression)
                    )
                }
            }
        }

        super.visitCallExpression(expression)
    }

    private fun checkForDuplicates(moduleCall: KtCallExpression) {
        val definitions = definitionsByModule[moduleCall] ?: return

        val grouped = definitions.groupBy { it.type }
        grouped.forEach { (type, defs) ->
            val unqualifiedCount = defs.count { !it.hasQualifier }
            val hasUnqualifiedViolation = if (allowOneDefault) unqualifiedCount > 1 else unqualifiedCount > 0
            if (defs.size > 1 && hasUnqualifiedViolation) {
                defs.first { !it.hasQualifier }.expression.let { expr ->
                    report(
                        CodeSmell(
                            issue,
                            Entity.from(expr),
                            """
                            Multiple definitions of '$type' without qualifiers
                            → Runtime DefinitionOverrideException when module loads
                            → Use named() to distinguish between instances

                            ✗ Bad:  single { HttpClient() }; single { HttpClient() }
                            ✓ Good: single(named("cio")) { HttpClient() }; single(named("okhttp")) { HttpClient() }
                            """.trimIndent()
                        )
                    )
                }
            }
        }
    }

    private fun findParentModule(expression: KtCallExpression): KtCallExpression? {
        var parent = expression.parent
        while (parent != null) {
            if (parent is KtCallExpression && parent.getCallNameExpression()?.text == "module") {
                return parent
            }
            parent = parent.parent
        }
        return null
    }

    private fun extractTypeName(expression: KtCallExpression): String? {
        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()
            ?: expression.valueArguments.firstOrNull()?.getArgumentExpression() as? KtLambdaExpression

        if (lambda != null) {
            val bodyExpr = lambda.bodyExpression?.statements?.firstOrNull() as? KtCallExpression
            return bodyExpr?.getCallNameExpression()?.text
        }

        val arg = expression.valueArguments.firstOrNull()?.getArgumentExpression()?.text
        return arg?.removePrefix("::")
    }

    private fun hasQualifierArgument(expression: KtCallExpression): Boolean {
        return expression.valueArguments.any { arg ->
            val argText = arg.getArgumentExpression()?.text ?: ""
            argText.contains("named(") || argText.contains("qualifier(") || argText.contains("StringQualifier(")
        }
    }
}
