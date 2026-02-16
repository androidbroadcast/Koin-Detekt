package io.github.krozov.detekt.koin.platform

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * Detects `startKoin()` called in Activity/Fragment instead of Application.
 *
 * Calling `startKoin {}` in Activity, Fragment, or Composable causes `KoinAppAlreadyStartedException`
 * on configuration changes (rotation, theme change) because Koin is reinitialized while already running.
 *
 * Reference: https://github.com/InsertKoinIO/koin/issues/1840
 *
 * <noncompliant>
 * class MainActivity : Activity() {
 *     override fun onCreate() {
 *         startKoin { modules(appModule) } // ❌ Will crash on rotation
 *     }
 * }
 * </noncompliant>
 *
 * <compliant>
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         startKoin { modules(appModule) } // ✓ Survives config changes
 *     }
 * }
 * </compliant>
 */
public class StartKoinInActivity(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "StartKoinInActivity",
        severity = Severity.Warning,
        description = "startKoin() in Activity/Fragment causes KoinAppAlreadyStartedException on config changes",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.calleeExpression?.text ?: return
        if (callName != "startKoin") return

        // Find containing class or function
        val containingClass = expression.parents.filterIsInstance<KtClass>().firstOrNull()

        var isFrameworkEntry = false
        var isApplication = false

        if (containingClass != null) {
            val superTypes = containingClass.superTypeListEntries.map { it.text }

            // Check if it's an Activity, Fragment
            val isActivity = superTypes.any { it.contains("Activity") }
            val isFragment = superTypes.any { it.contains("Fragment") }

            // Check if it's an Application
            isApplication = superTypes.any { it.contains("Application") }

            isFrameworkEntry = isActivity || isFragment
        }

        // Also check if inside a @Composable function
        val containingFunction = expression.parents.filterIsInstance<org.jetbrains.kotlin.psi.KtNamedFunction>().firstOrNull()
        if (containingFunction != null) {
            val isComposable = containingFunction.annotationEntries.any { it.shortName?.asString() == "Composable" }
            if (isComposable) {
                isFrameworkEntry = true
            }
        }

        if (isFrameworkEntry && !isApplication) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    startKoin in Activity/Fragment → KoinAppAlreadyStartedException on config change
                    → Call startKoin in Application.onCreate() instead

                    ✗ Bad:  class MainActivity : Activity() { startKoin {} }
                    ✓ Good: class MyApp : Application() { startKoin {} }
                    """.trimIndent()
                )
            )
        }
    }
}
