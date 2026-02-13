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

internal class NoKoinGetInApplication(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "NoKoinGetInApplication",
        severity = Severity.Warning,
        description = "Detects get() or inject() calls inside startKoin or koinConfiguration blocks. " +
                "Use modules() to define dependencies instead.",
        debt = Debt.TEN_MINS
    )

    private var insideStartKoinBlock = false

    override fun visitCallExpression(expression: KtCallExpression) {
        val callName = expression.getCallNameExpression()?.text

        // Track entering startKoin/koinConfiguration blocks
        if (callName in setOf("startKoin", "koinConfiguration")) {
            val wasInside = insideStartKoinBlock
            insideStartKoinBlock = true
            super.visitCallExpression(expression)
            insideStartKoinBlock = wasInside
            return
        }

        // Check for get()/inject() inside application blocks
        if (callName in setOf("get", "inject") && insideStartKoinBlock) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    $callName() used in startKoin block → Service locator at app initialization
                    → Define dependencies in modules instead

                    ✗ Bad:  startKoin { val api = get<Api>() }
                    ✓ Good: startKoin { modules(appModule) }
                    """.trimIndent()
                )
            )
        }

        super.visitCallExpression(expression)
    }
}
