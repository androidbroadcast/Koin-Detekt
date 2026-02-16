package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.*

internal class DuplicateBindingWithoutQualifier(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "DuplicateBindingWithoutQualifier",
        severity = Severity.Warning,
        description = "Detects multiple bindings to same type without qualifiers (silent override)",
        debt = Debt.TEN_MINS
    )

    private val bindingsInCurrentModule = mutableMapOf<String, MutableList<KtBinaryExpression>>()

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        // Track module scope - clear when entering new module
        if (expression.calleeExpression?.text == "module") {
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
            bindingsInCurrentModule.getOrPut(boundType) { mutableListOf() }.add(expression)

            // Report if duplicate
            if (bindingsInCurrentModule[boundType]!!.size > 1) {
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
