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
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
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
    private val localFunctionNames = mutableSetOf<String>()

    override fun visitKtFile(file: KtFile) {
        // Collect all locally defined function names to avoid false positives
        localFunctionNames.clear()
        localFunctionNames.addAll(
            file.collectDescendantsOfType<KtNamedFunction>()
                .mapNotNull { it.name }
        )
        super.visitKtFile(file)
    }

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

        // Inside startKoin: check for Koin get()/inject() (top-level calls without receiver)
        if (callName in setOf("get", "inject") && insideStartKoinBlock) {
            // Skip if this is a locally defined function (not from Koin)
            if (callName in localFunctionNames) {
                super.visitCallExpression(expression)
                return
            }

            // Check if this is a receiver call (e.g., map.get()) vs top-level call (e.g., get<T>())
            val parent = expression.parent
            if (parent !is KtDotQualifiedExpression || parent.receiverExpression == expression) {
                // This is a top-level call (get<T>()) or the call IS the receiver - report it
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
            // If it's a receiver call (map.get()), don't report (it's not Koin)
        }

        super.visitCallExpression(expression)
    }
}
