package io.github.krozov.detekt.koin.platform

import io.github.krozov.detekt.koin.util.ImportAwareRule
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
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
 *         startKoin { modules(appModule) } // Will crash on rotation
 *     }
 * }
 * </noncompliant>
 *
 * <compliant>
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         startKoin { modules(appModule) } // Survives config changes
 *     }
 * }
 * </compliant>
 */
internal class StartKoinInActivity(config: Config = Config.empty) : ImportAwareRule(config) {
    private val activityTypes = setOf(
        "Activity", "AppCompatActivity", "FragmentActivity", "ComponentActivity"
    )
    private val fragmentTypes = setOf(
        "Fragment", "DialogFragment", "BottomSheetDialogFragment"
    )
    private val applicationTypes = setOf(
        "Application", "MultiDexApplication"
    )

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
        // startKoin is from org.koin.core.context — skip if imported from elsewhere
        val fqns = importContext.resolveFqn(callName)
        if (fqns.isNotEmpty() && fqns.none { it.startsWith("org.koin.") }) return

        val containingClass = expression.parents.filterIsInstance<KtClass>().firstOrNull()

        var isFrameworkEntry = false
        var isApplication = false

        if (containingClass != null) {
            // Extract simple supertype names: strip package prefix, generic args, and constructor calls
            val superTypeNames = containingClass.superTypeListEntries
                .mapNotNull { it.typeReference?.text }
                .map { it.substringBefore('(').substringBefore('<').substringAfterLast('.') }

            val isActivity = superTypeNames.any { it in activityTypes }
            val isFragment = superTypeNames.any { it in fragmentTypes }
            isApplication = superTypeNames.any { it in applicationTypes }

            isFrameworkEntry = isActivity || isFragment
        }

        // Also check if inside a @Composable function
        val containingFunction = expression.parents
            .filterIsInstance<KtNamedFunction>()
            .firstOrNull()
        if (containingFunction != null) {
            val isComposable = containingFunction.annotationEntries
                .any { it.shortName?.asString() == "Composable" }
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
