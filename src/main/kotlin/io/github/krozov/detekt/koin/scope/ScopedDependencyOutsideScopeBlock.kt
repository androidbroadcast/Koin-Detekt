package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

internal class ScopedDependencyOutsideScopeBlock(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "ScopedDependencyOutsideScopeBlock",
        severity = Severity.Warning,
        description = "Detects scoped {} or scopedOf() outside a scope {} / activityScope {} block. " +
                "Scoped dependencies must be defined within a scope block.",
        debt = Debt.TEN_MINS
    )

    private var insideScopeBlock = false
    private val scopeBlockFunctions = setOf("scope", "activityScope", "fragmentScope", "viewModelScope", "requestScope")

    override fun visitCallExpression(expression: KtCallExpression) {
        val callName = expression.getCallNameExpression()?.text

        // Track entering scope blocks
        if (callName in scopeBlockFunctions) {
            val wasInside = insideScopeBlock
            insideScopeBlock = true
            super.visitCallExpression(expression)
            insideScopeBlock = wasInside
            return
        }

        // Check for scoped definitions outside scope blocks
        if (callName in setOf("scoped", "scopedOf") && !insideScopeBlock) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "'$callName' must be defined inside a scope {} block (scope, activityScope, fragmentScope, etc.)."
                )
            )
        }

        super.visitCallExpression(expression)
    }
}
