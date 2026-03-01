package io.github.krozov.detekt.koin.scope

import io.github.krozov.detekt.koin.util.ImportAwareRule
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

internal class ScopedDependencyOutsideScopeBlock(config: Config) : ImportAwareRule(config) {

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

        // Track entering scope blocks.
        // Use resolveFqn with org.koin prefix to cover all Koin packages including
        // org.koin.androidx.scope (activityScope, fragmentScope) which are not in KOIN_PACKAGES.
        if (callName in scopeBlockFunctions) {
            val fqns = importContext.resolveFqn(callName!!)
            val confirmedNonKoin = fqns.isNotEmpty() && fqns.none { it.startsWith("org.koin.") }
            if (confirmedNonKoin) {
                super.visitCallExpression(expression)
                return
            }
            val wasInside = insideScopeBlock
            insideScopeBlock = true
            super.visitCallExpression(expression)
            insideScopeBlock = wasInside
            return
        }

        // Check for scoped definitions outside scope blocks.
        if (callName in setOf("scoped", "scopedOf") && !insideScopeBlock) {
            val fqns = importContext.resolveFqn(callName!!)
            val confirmedNonKoin = fqns.isNotEmpty() && fqns.none { it.startsWith("org.koin.") }
            if (confirmedNonKoin) {
                super.visitCallExpression(expression)
                return
            }
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    Scoped dependency outside scope block → Scoped lifecycle has no scope to bind to
                    → Define inside scope { }, activityScope { }, or other scope blocks

                    ✗ Bad:  module { scoped { UserSession() } }
                    ✓ Good: module { scope<MainActivity> { scoped { UserSession() } } }
                    """.trimIndent()
                )
            )
        }

        super.visitCallExpression(expression)
    }
}
