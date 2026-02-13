package io.github.krozov.detekt.koin.platform.ktor

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Detects `install(Koin)` in routing blocks or route handlers.
 *
 * Koin should be initialized once at application level, not per-route.
 * Multiple Koin installations cause configuration conflicts.
 *
 * <noncompliant>
 * fun Application.module() {
 *     routing {
 *         install(Koin) { } // Wrong!
 *         get("/api") { }
 *     }
 * }
 * </noncompliant>
 *
 * <compliant>
 * fun Application.module() {
 *     install(Koin) { }
 *     routing {
 *         get("/api") { }
 *     }
 * }
 * </compliant>
 */
public class KtorApplicationKoinInit(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "KtorApplicationKoinInit",
        severity = Severity.Warning,
        description = "install(Koin) should be at Application level, not in routing",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.calleeExpression?.text ?: return

        // Check install(Koin) calls
        if (callName == "install") {
            val typeArg = expression.valueArguments.firstOrNull()?.text
            if (typeArg == "Koin") {
                // Check if this install(Koin) is inside a routing {} lambda
                if (isInsideRoutingBlock(expression)) {
                    report(
                        CodeSmell(
                            issue,
                            Entity.from(expression),
                            "install(Koin) called inside routing block. " +
                                    "Koin should be initialized at Application level. " +
                                    "Move install(Koin) outside routing {} block."
                        )
                    )
                }
            }
        }
    }

    private fun isInsideRoutingBlock(expression: KtCallExpression): Boolean {
        var current = expression.parent
        while (current != null) {
            if (current is KtLambdaExpression) {
                // Check if this lambda belongs to a routing call
                val lambdaParent = current.parent?.parent
                if (lambdaParent is KtCallExpression) {
                    val parentCallName = lambdaParent.calleeExpression?.text
                    if (parentCallName == "routing") {
                        return true
                    }
                }
            }
            current = current.parent
        }
        return false
    }
}
