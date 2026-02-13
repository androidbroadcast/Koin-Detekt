package io.github.krozov.detekt.koin.platform.android

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
 * Detects `androidContext()` / `androidApplication()` called outside `startKoin`.
 *
 * These should only be set once at app initialization in Application.onCreate.
 * Calling them elsewhere may set wrong context or cause initialization issues.
 *
 * <noncompliant>
 * val myModule = module {
 *     single { androidContext() } // Wrong!
 * }
 * </noncompliant>
 *
 * <compliant>
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         startKoin {
 *             androidContext(this@MyApp)
 *             modules(appModule)
 *         }
 *     }
 * }
 * </compliant>
 */
public class AndroidContextNotFromKoin(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "AndroidContextNotFromKoin",
        severity = Severity.Warning,
        description = "androidContext/androidApplication should only be called in startKoin",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.calleeExpression?.text ?: return
        if (callName != "androidContext" && callName != "androidApplication") return

        // Check if inside startKoin lambda
        var current = expression.parent
        var inStartKoin = false
        while (current != null) {
            if (current is KtLambdaExpression) {
                val parentCall = current.parent?.parent as? KtCallExpression
                if (parentCall?.calleeExpression?.text == "startKoin") {
                    inStartKoin = true
                    break
                }
            }
            current = current.parent
        }

        if (!inStartKoin) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    $callName() outside startKoin {} → May set wrong context, initialization errors
                    → Call only once in Application.onCreate inside startKoin block

                    ✗ Bad:  val myModule = module { single { androidContext() } }
                    ✓ Good: class MyApp : Application() { override fun onCreate() { startKoin { androidContext(this@MyApp) } } }
                    """.trimIndent()
                )
            )
        }
    }
}
