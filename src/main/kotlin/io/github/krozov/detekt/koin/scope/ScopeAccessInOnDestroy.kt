package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtNamedFunction

internal class ScopeAccessInOnDestroy(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "ScopeAccessInOnDestroy",
        severity = Severity.Warning,
        description = "Detects scope access (get(), inject()) in onDestroy/onDestroyView methods. " +
                "The scope may already be closed, causing ClosedScopeException.",
        debt = Debt.FIVE_MINS
    )

    private val destroyMethods = setOf("onDestroy", "onDestroyView")

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        if (function.name !in destroyMethods) return

        // Check if function body contains get() or inject() calls
        val bodyText = function.bodyExpression?.text ?: return
        val hasScopeAccess = bodyText.contains("get<") ||
                            bodyText.contains("get(") ||
                            bodyText.contains("inject<") ||
                            bodyText.contains("inject(")

        if (hasScopeAccess) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(function),
                    """
                    Scope access in ${function.name}() → ClosedScopeException risk
                    → Access dependencies in onCreate/onCreateView instead

                    ✗ Bad:  fun onDestroy() { val s = get<Service>() }
                    ✓ Good: lateinit var s: Service
                            fun onCreate() { s = get<Service>() }
                    """.trimIndent()
                )
            )
        }
    }
}
