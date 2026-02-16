package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression

/**
 * Detects excessive use of `createdAtStart = true` in Koin modules.
 *
 * Having many singletons eagerly initialized at app startup can cause ANR (Application Not Responding)
 * on Android, especially on lower-end devices. This rule warns when a module contains more than
 * the configured threshold (default: 10) of definitions with `createdAtStart = true`.
 *
 * Reference: https://github.com/InsertKoinIO/koin/issues/2266
 *
 * <noncompliant>
 * val appModule = module {
 *     single(createdAtStart = true) { Service1() }
 *     single(createdAtStart = true) { Service2() }
 *     // ... 11+ total createdAtStart definitions
 *     single(createdAtStart = true) { Service11() }
 * }
 * </noncompliant>
 *
 * <compliant>
 * val appModule = module {
 *     // Only critical services created at start
 *     single(createdAtStart = true) { LoggingService() }
 *     single(createdAtStart = true) { CrashReporter() }
 *
 *     // Others lazily initialized
 *     single { DatabaseService() }
 *     single { NetworkService() }
 * }
 * </compliant>
 */
public class ExcessiveCreatedAtStart(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "ExcessiveCreatedAtStart",
        severity = Severity.Warning,
        description = "Too many createdAtStart definitions can cause ANR on app startup",
        debt = Debt.TEN_MINS
    )

    private val maxCreatedAtStart: Int = valueOrDefault("maxCreatedAtStart", 10)

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.calleeExpression?.text ?: return

        // Only process module {} calls
        if (callName != "module") return

        // Get the lambda argument
        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()
            ?: expression.valueArguments.firstOrNull()?.getArgumentExpression() as? KtLambdaExpression
            ?: return

        val bodyExpression = lambda.bodyExpression ?: return

        // Count createdAtStart = true definitions in this module
        var count = 0
        bodyExpression.accept(object : org.jetbrains.kotlin.psi.KtVisitorVoid() {
            override fun visitCallExpression(callExpression: KtCallExpression) {
                super.visitCallExpression(callExpression)

                val definitionCallName = callExpression.calleeExpression?.text
                if (definitionCallName in setOf("single", "factory", "scoped")) {
                    val hasCreatedAtStart = callExpression.valueArguments.any { arg ->
                        val argName = arg.getArgumentName()?.asName?.asString()
                        if (argName == "createdAtStart") {
                            val argValue = arg.getArgumentExpression()?.text
                            return@any argValue == "true"
                        }
                        false
                    }
                    if (hasCreatedAtStart) {
                        count++
                    }
                }
            }
        })

        if (count > maxCreatedAtStart) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    Excessive createdAtStart ($count) → ANR risk on startup
                    → Consider lazy initialization for non-critical services
                    → Maximum recommended: $maxCreatedAtStart per module

                    ✗ Bad:  11+ single(createdAtStart = true) in one module
                    ✓ Good: Limit eager initialization to critical services only
                    """.trimIndent()
                )
            )
        }
    }
}
