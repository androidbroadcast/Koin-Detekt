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

class KtorRequestScopeMisuse(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "KtorRequestScopeMisuse",
        severity = Severity.Warning,
        description = "Detects single {} or singleOf() inside requestScope {} in Ktor. " +
                "Singleton in a request scope is semantically incorrect.",
        debt = Debt.TEN_MINS
    )

    private var insideRequestScope = false

    override fun visitCallExpression(expression: KtCallExpression) {
        val callName = expression.getCallNameExpression()?.text

        // Track entering requestScope blocks
        if (callName == "requestScope") {
            val wasInside = insideRequestScope
            insideRequestScope = true
            super.visitCallExpression(expression)
            insideRequestScope = wasInside
            return
        }

        // Check for single inside requestScope
        if (callName in setOf("single", "singleOf") && insideRequestScope) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "'$callName' inside requestScope is semantically incorrect. " +
                            "A singleton should not be scoped to a request. Use scoped {} instead."
                )
            )
        }

        super.visitCallExpression(expression)
    }
}
