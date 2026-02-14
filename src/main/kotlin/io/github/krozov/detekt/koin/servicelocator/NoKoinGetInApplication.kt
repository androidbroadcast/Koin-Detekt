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
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNamedFunction
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
    // Track Koin function imports per file (cleared in visitKtFile)
    private val koinImports = mutableSetOf<String>()
    // Track top-level function names to avoid false positives
    private val topLevelFunctionNames = mutableSetOf<String>()

    override fun visitKtFile(file: KtFile) {
        koinImports.clear()
        insideStartKoinBlock = false
        // Collect only top-level function names to avoid false negatives
        topLevelFunctionNames.clear()
        topLevelFunctionNames.addAll(
            file.declarations.filterIsInstance<KtNamedFunction>().mapNotNull { it.name }
        )
        super.visitKtFile(file)
    }

    override fun visitImportDirective(importDirective: KtImportDirective) {
        val importPath = importDirective.importPath
        val pathStr = importPath?.pathStr
        if (pathStr?.startsWith("org.koin.") == true) {
            val importedName = importDirective.importedName?.asString()
            if (importedName != null) {
                koinImports.add(importedName)
            } else if (importPath.isAllUnder) {
                // Star import from Koin package: assume common service locator functions may be used
                koinImports.addAll(listOf("get", "inject"))
            }
        }
        super.visitImportDirective(importDirective)
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

        // Inside startKoin: check for Koin get()/inject() using import-based detection
        if (callName in setOf("get", "inject") && insideStartKoinBlock) {
            // Skip if this is a locally defined top-level function (not from Koin)
            if (callName in topLevelFunctionNames) {
                super.visitCallExpression(expression)
                return
            }

            // Check if this is a receiver call (e.g., map.get()) vs top-level call (e.g., get<T>())
            val parent = expression.parent
            val isReceiverCall = parent is KtDotQualifiedExpression && parent.receiverExpression != expression

            // Report only if: imported from Koin AND not a receiver call
            if (callName in koinImports && !isReceiverCall) {
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
        }

        super.visitCallExpression(expression)
    }
}
