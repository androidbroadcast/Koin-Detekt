package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

internal class NoInjectDelegate(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "NoInjectDelegate",
        severity = Severity.Warning,
        description = "Detects inject() property delegate usage (service locator pattern). " +
                "Use constructor injection instead of property delegation with inject().",
        debt = Debt.TWENTY_MINS
    )

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)

        // Check if the property has a delegate
        val delegate = property.delegate ?: return

        // Check for direct inject() call: by inject()
        val delegateExpression = delegate.expression
        val callName = when (delegateExpression) {
            is KtCallExpression -> delegateExpression.getCallNameExpression()?.text
            is KtDotQualifiedExpression -> {
                // Check for qualified inject() call: by scope.inject()
                val selectorCall = delegateExpression.selectorExpression as? KtCallExpression
                selectorCall?.getCallNameExpression()?.text
            }
            else -> null
        }

        if (callName in setOf("inject", "injectOrNull")) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(property),
                    "Avoid using $callName() property delegate. Use constructor injection instead."
                )
            )
        }
    }
}
