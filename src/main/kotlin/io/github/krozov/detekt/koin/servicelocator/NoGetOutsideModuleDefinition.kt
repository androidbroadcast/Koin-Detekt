package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
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
    // Track Koin function imports per file (cleared in visitKtFile)
    private val koinImports = mutableSetOf<String>()

    override fun visitKtFile(file: KtFile) {
        koinImports.clear()
        insideDefinitionBlock = false
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
                koinImports.addAll(listOf("get", "getOrNull", "getAll"))
            }
        }
        super.visitImportDirective(importDirective)
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

        // Check for get() calls from Koin (must be imported AND outside definition block)
        if (callName in setOf("get", "getOrNull", "getAll") && !insideDefinitionBlock && callName in koinImports) {
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
