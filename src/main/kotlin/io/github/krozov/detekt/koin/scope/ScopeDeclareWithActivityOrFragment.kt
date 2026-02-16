package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * Detects `scope.declare()` called with Activity or Fragment instances.
 *
 * Using `scope.declare(activity)` or `scope.declare(fragment)` causes memory leaks because
 * the declaration is not automatically cleared when the scope closes. The Activity/Fragment
 * instance remains referenced by Koin even after it should be garbage collected.
 *
 * Reference: https://github.com/InsertKoinIO/koin/issues/1122
 *
 * <noncompliant>
 * class MainActivity : AppCompatActivity() {
 *     fun setupScope() {
 *         val scope = getKoin().createScope("my_scope", named("activity"))
 *         scope.declare(this) // ❌ Memory leak: Activity never released
 *     }
 * }
 * </noncompliant>
 *
 * <compliant>
 * class MainActivity : AppCompatActivity() {
 *     fun setupScope() {
 *         // Use androidScope() extension or manual scope lifecycle management
 *         val scope = androidScope()
 *         // Don't declare Activity/Fragment instances in scopes
 *     }
 * }
 * </compliant>
 */
public class ScopeDeclareWithActivityOrFragment(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "ScopeDeclareWithActivityOrFragment",
        severity = Severity.Warning,
        description = "scope.declare() with Activity/Fragment causes memory leaks",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.calleeExpression?.text ?: return
        if (callName != "declare") return

        // Check the argument passed to declare()
        val argument = expression.valueArguments.firstOrNull()?.getArgumentExpression()?.text ?: return

        // Check if argument contains "activity" or "fragment" (case-insensitive)
        val suspiciousArgument = argument.contains("activity", ignoreCase = true) ||
                argument.contains("fragment", ignoreCase = true) ||
                argument == "this"

        // If argument is "this", check if we're inside Activity/Fragment class
        val isThisInActivityOrFragment = if (argument == "this") {
            val containingClass = expression.parents.filterIsInstance<org.jetbrains.kotlin.psi.KtClass>().firstOrNull()
            containingClass?.superTypeListEntries?.any {
                it.text.contains("Activity") || it.text.contains("Fragment")
            } ?: false
        } else {
            false
        }

        if (suspiciousArgument && (argument != "this" || isThisInActivityOrFragment)) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    scope.declare(activity/fragment) → Memory leak (declaration not cleared on scope close)
                    → Use androidScope() or avoid declaring Activity/Fragment instances

                    ✗ Bad:  scope.declare(activity)
                    ✓ Good: val scope = androidScope() // Handles lifecycle automatically
                    """.trimIndent()
                )
            )
        }
    }
}
