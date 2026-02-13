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

internal class FactoryInScopeBlock(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "FactoryInScopeBlock",
        severity = Severity.Style,
        description = "Detects factory {} or factoryOf() inside scope {} blocks. " +
                "Factory creates a new instance on every call regardless of scope, which may be unintended.",
        debt = Debt.FIVE_MINS
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

        // Check for factory inside scope blocks
        if (callName in setOf("factory", "factoryOf") && insideScopeBlock) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    Factory inside scope block → Creates new instance every time, ignores scope lifecycle
                    → Use scoped { } to respect scope lifecycle, or move factory outside scope block

                    ✗ Bad:  scope<Activity> { factory { Presenter() } }
                    ✓ Good: scope<Activity> { scoped { Presenter() } }
                    """.trimIndent()
                )
            )
        }

        super.visitCallExpression(expression)
    }
}
