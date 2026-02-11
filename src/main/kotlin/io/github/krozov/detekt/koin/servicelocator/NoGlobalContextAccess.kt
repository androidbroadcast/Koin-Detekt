package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class NoGlobalContextAccess(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "NoGlobalContextAccess",
        severity = Severity.Warning,
        description = "Detects direct access to GlobalContext.get() or KoinPlatformTools. " +
                "This is the most egregious service locator variant.",
        debt = Debt.TWENTY_MINS
    )

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        val receiverText = (expression.receiverExpression as? KtNameReferenceExpression)?.text

        if (receiverText in setOf("GlobalContext", "KoinPlatformTools")) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Avoid direct access to $receiverText. Use dependency injection instead."
                )
            )
        }
    }
}
