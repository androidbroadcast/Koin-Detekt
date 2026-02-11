package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

internal class NoGetOutsideModuleDefinition(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "NoGetOutsideModuleDefinition",
        severity = Severity.Warning,
        description = "Detects get() calls outside Koin module definition blocks. " +
                "Use constructor injection instead of service locator pattern.",
        debt = Debt.TWENTY_MINS
    )

    private var insideDefinitionBlock = false
    private val definitionFunctions = setOf("single", "factory", "scoped", "viewModel", "worker")

    override fun visitCallExpression(expression: KtCallExpression) {
        val callName = expression.getCallNameExpression()?.text

        // Track entering definition blocks
        if (callName in definitionFunctions) {
            val wasInside = insideDefinitionBlock
            insideDefinitionBlock = true
            super.visitCallExpression(expression)
            insideDefinitionBlock = wasInside
            return
        }

        // Check for get() calls
        if (callName in setOf("get", "getOrNull", "getAll") && !insideDefinitionBlock) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Avoid using $callName() outside module definitions. Use constructor injection instead."
                )
            )
        }

        super.visitCallExpression(expression)
    }
}
