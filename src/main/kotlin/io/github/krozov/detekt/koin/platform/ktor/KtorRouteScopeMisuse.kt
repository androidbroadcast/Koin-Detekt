package io.github.krozov.detekt.koin.platform.ktor

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Detects incorrect usage of `koinScope()` in Ktor routes.
 *
 * Scopes created outside route handlers are shared across requests,
 * causing state leaks. Each request must have isolated scope.
 *
 * <noncompliant>
 * val sharedScope = koinScope() // Shared across requests!
 * get("/api") {
 *     val service = sharedScope.get<Service>()
 * }
 * </noncompliant>
 *
 * <compliant>
 * get("/api") {
 *     call.koinScope().get<Service>() // Request-scoped
 * }
 * </compliant>
 */
public class KtorRouteScopeMisuse(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "KtorRouteScopeMisuse",
        severity = Severity.Warning,
        description = "koinScope() should be request-scoped, not shared",
        debt = Debt.TEN_MINS
    )

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)

        val initializer = property.initializer as? KtCallExpression ?: return
        val callName = initializer.calleeExpression?.text ?: return

        if (callName == "koinScope") {
            // Check if it's call.koinScope() - that's ok
            val receiver = (initializer.parent as? KtDotQualifiedExpression)?.receiverExpression?.text
            if (receiver != "call") {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(property),
                        """
                        koinScope() stored in property → Shared scope across all requests, state leaks
                        → Use call.koinScope() inside route handler for per-request isolation

                        ✗ Bad:  val scope = koinScope(); get("/api") { scope.get<Service>() }
                        ✓ Good: get("/api") { call.koinScope().get<Service>() }
                        """.trimIndent()
                    )
                )
            }
        }
    }
}
