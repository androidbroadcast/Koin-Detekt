package io.github.krozov.detekt.koin.moduledsl

import io.github.krozov.detekt.koin.util.ImportAwareRule
import io.github.krozov.detekt.koin.util.Resolution
import io.github.krozov.detekt.koin.util.resolveKoin
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile

internal class DuplicateBindingWithoutQualifier(config: Config) : ImportAwareRule(config) {

    override val issue = Issue(
        id = "DuplicateBindingWithoutQualifier",
        severity = Severity.Warning,
        description = "Detects multiple bindings to same type without qualifiers (silent override)",
        debt = Debt.TEN_MINS
    )

    private val bindingsInCurrentModule = mutableMapOf<String, MutableList<KtBinaryExpression>>()

    override fun visitKtFile(file: KtFile) {
        bindingsInCurrentModule.clear()
        super.visitKtFile(file)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        // Track module scope - clear when entering new module
        if (expression.calleeExpression?.text == "module") {
            if (importContext.resolveKoin("module") == Resolution.NOT_KOIN) return
            bindingsInCurrentModule.clear()
        }
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        super.visitBinaryExpression(expression)

        // Find "bind X::class" infix patterns
        if (expression.operationReference.text != "bind") return

        // Extract the bound type name
        val rightSide = expression.right?.text ?: return
        val boundType = rightSide.removeSuffix("::class").trim()

        // Check if this binding has a qualifier
        // We need to look at the parent context for "named()" calls after "bind"
        val parent = expression.parent
        val fullText = parent?.text ?: expression.text
        val hasQualifier = fullText.contains(Regex("""named\s*\(""")) ||
                          fullText.contains(Regex("""qualifier\s*="""))

        if (!hasQualifier) {
            val bindings = bindingsInCurrentModule.getOrPut(boundType) { mutableListOf() }
            bindings.add(expression)

            // Report if duplicate
            if (bindings.size > 1) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(expression),
                        """
                        Duplicate binding to $boundType without qualifier → Silent override
                        → Add named() qualifiers to keep both bindings

                        ✗ Bad:  single { A() } bind Foo::class
                                single { B() } bind Foo::class
                        ✓ Good: single { A() } bind Foo::class named("a")
                                single { B() } bind Foo::class named("b")
                        """.trimIndent()
                    )
                )
            }
        }
    }
}
