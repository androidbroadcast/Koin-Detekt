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
 * Detects `androidContext()` / `androidApplication()` called outside Koin context.
 *
 * These functions should only be called inside `startKoin {}` or Koin module definition blocks
 * (`module {}`, `single {}`, `factory {}`, `scoped {}`, `viewModel {}`, `worker {}`).
 *
 * <noncompliant>
 * fun setupContext() {
 *     val ctx = androidContext() // Wrong — not in Koin context
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
 *
 * val myModule = module {
 *     single { MyRepository(androidContext()) } // OK — inside module definition
 * }
 * </compliant>
 */
public class AndroidContextNotFromKoin(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "AndroidContextNotFromKoin",
        severity = Severity.Warning,
        description = "androidContext/androidApplication should only be called in startKoin or module definitions",
        debt = Debt.FIVE_MINS
    )

    private val validParentCalls: Set<String> = setOf(
        "startKoin", "module", "single", "factory", "scoped", "viewModel", "worker"
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.calleeExpression?.text ?: return
        if (callName != "androidContext" && callName != "androidApplication") return

        // Check if inside a valid Koin context (startKoin or module definitions)
        var current = expression.parent
        var inKoinContext = false
        while (current != null) {
            if (current is KtLambdaExpression) {
                val parentCall = current.parent?.parent as? KtCallExpression
                if (parentCall?.calleeExpression?.text in validParentCalls) {
                    inKoinContext = true
                    break
                }
            }
            current = current.parent
        }

        if (!inKoinContext) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    $callName() outside Koin context → May set wrong context, initialization errors
                    → Call inside startKoin {} or module definition blocks

                    ✗ Bad:  fun setup() { val ctx = androidContext() }
                    ✓ Good: startKoin { androidContext(this@MyApp) } or module { single { MyRepo(androidContext()) } }
                    """.trimIndent()
                )
            )
        }
    }
}
