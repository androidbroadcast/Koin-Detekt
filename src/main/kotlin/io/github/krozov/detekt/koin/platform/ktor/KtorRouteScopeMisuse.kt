package io.github.krozov.detekt.koin.platform.ktor

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
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
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
internal class KtorRouteScopeMisuse(config: Config = Config.empty) : ImportAwareRule(config) {
    override val issue: Issue = Issue(
        id = "KtorRouteScopeMisuse",
        severity = Severity.Warning,
        description = "koinScope() should be request-scoped, not shared",
        debt = Debt.TEN_MINS
    )

    private fun extractReceiverName(receiver: KtExpression): String? = when (receiver) {
        is KtNameReferenceExpression -> receiver.getReferencedName()
        is KtParenthesizedExpression -> receiver.expression?.let { extractReceiverName(it) }
        else -> null
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)

        val initializer = property.initializer ?: return

        // Determine the call name and receiver expression depending on whether the
        // initializer is a plain call (`koinScope()`) or dot-qualified (`x.koinScope()`).
        val callName: String
        val receiverExpression: KtExpression?

        when (initializer) {
            is KtDotQualifiedExpression -> {
                val selector = initializer.selectorExpression as? KtCallExpression ?: return
                callName = selector.calleeExpression?.text ?: return
                receiverExpression = initializer.receiverExpression
            }
            is KtCallExpression -> {
                callName = initializer.calleeExpression?.text ?: return
                receiverExpression = null
            }
            else -> return
        }

        if (callName != "koinScope") return
        if (importContext.resolveKoin(callName) == Resolution.NOT_KOIN) return

        // call.koinScope() is the intended Ktor pattern — do not report.
        // Use PSI-based name extraction to handle parenthesised receivers like (call).koinScope().
        if (receiverExpression != null && extractReceiverName(receiverExpression) == "call") return

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
