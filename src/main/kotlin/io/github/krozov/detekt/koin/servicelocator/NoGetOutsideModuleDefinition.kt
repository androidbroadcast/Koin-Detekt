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
            // Skip qualified calls like alarmDao.getAll() -- those are method calls on arbitrary
            // objects, not Koin service locator usage. Koin's get() is always unqualified.
            if (expression.parent is KtDotQualifiedExpression &&
                (expression.parent as KtDotQualifiedExpression).selectorExpression == expression) {
                super.visitCallExpression(expression)
                return
            }

            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    $callName() used outside module definition → Service locator pattern, harder to test
                    → Use constructor injection instead

                    ✗ Bad:  class MyRepo : KoinComponent { val api = get<Api>() }
                    ✓ Good: class MyRepo(private val api: Api)
                    """.trimIndent()
                )
            )
        }

        super.visitCallExpression(expression)
    }
}
