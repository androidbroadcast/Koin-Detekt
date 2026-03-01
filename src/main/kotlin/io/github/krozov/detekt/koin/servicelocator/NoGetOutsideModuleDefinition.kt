package io.github.krozov.detekt.koin.servicelocator

import io.github.krozov.detekt.koin.util.Resolution
import io.github.krozov.detekt.koin.util.ImportAwareRule
import io.github.krozov.detekt.koin.util.resolveKoin
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

internal class NoGetOutsideModuleDefinition(config: Config) : ImportAwareRule(config) {

    override val issue: Issue = Issue(
        id = "NoGetOutsideModuleDefinition",
        severity = Severity.Warning,
        description = "Detects get() calls outside Koin module definition blocks. " +
                "Use constructor injection instead of service locator pattern.",
        debt = Debt.TWENTY_MINS
    )

    private var insideDefinitionBlock = false
    private val definitionFunctions = setOf("single", "factory", "scoped", "viewModel", "worker")

    override fun visitKtFile(file: KtFile) {
        insideDefinitionBlock = false
        super.visitKtFile(file)
    }

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
            if (importContext.resolveKoin(callName ?: return) == Resolution.NOT_KOIN) {
                super.visitCallExpression(expression)
                return
            }
            // Skip qualified calls like obj.get() or obj?.get() — those are method calls on
            // arbitrary objects, not Koin service locator usage. Koin's get() is always unqualified.
            val parent = expression.parent
            val isQualified = when (parent) {
                is KtDotQualifiedExpression -> parent.selectorExpression == expression
                is KtSafeQualifiedExpression -> parent.selectorExpression == expression
                else -> false
            }
            if (isQualified) {
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
