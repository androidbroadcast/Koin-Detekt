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
        super.visitKtFile(file)
    }

    override fun visitImportDirective(importDirective: KtImportDirective) {
        val importPath = importDirective.importPath?.pathStr
        if (importPath?.startsWith("org.koin.") == true) {
            importDirective.importedName?.asString()?.let { koinImports.add(it) }
        }
        super.visitImportDirective(importDirective)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        val callName = expression.getCallNameExpression()?.text

        // Check if function is imported from Koin - skip if not
        if (callName != null && callName in setOf("get", "getOrNull", "getAll") && callName !in koinImports) {
            super.visitCallExpression(expression)
            return
        }

        // Track entering definition blocks
        if (callName in definitionFunctions) {
            val wasInside = insideDefinitionBlock
            insideDefinitionBlock = true
            super.visitCallExpression(expression)
            insideDefinitionBlock = wasInside
            return
        }

        // Check for get() calls from Koin
        if (callName in setOf("get", "getOrNull", "getAll") && !insideDefinitionBlock) {
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
