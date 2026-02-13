package io.github.krozov.detekt.koin.platform.compose

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Detects `koinViewModel()` calls outside `@Composable` functions.
 *
 * `koinViewModel()` requires Composition context and will crash at runtime
 * if called outside a Composable function.
 *
 * <noncompliant>
 * fun MyScreen() {
 *     val vm = koinViewModel<MyVM>() // Runtime crash!
 * }
 * </noncompliant>
 *
 * <compliant>
 * @Composable
 * fun MyScreen() {
 *     val vm = koinViewModel<MyVM>()
 * }
 * </compliant>
 */
public class KoinViewModelOutsideComposable(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "KoinViewModelOutsideComposable",
        severity = Severity.Warning,
        description = "koinViewModel() must be called inside @Composable function",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.calleeExpression?.text ?: return
        if (callName != "koinViewModel") return

        val containingFunction = expression.getStrictParentOfType<KtNamedFunction>()

        // If no containing function (e.g., in init block or property), report it
        if (containingFunction == null) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    koinViewModel() called outside @Composable → Runtime crash, requires Composition context
                    → Add @Composable annotation to the function

                    ✗ Bad:  fun MyScreen() { val vm = koinViewModel<MyVM>() }
                    ✓ Good: @Composable fun MyScreen() { val vm = koinViewModel<MyVM>() }
                    """.trimIndent()
                )
            )
            return
        }

        val annotations = containingFunction.annotationEntries.mapNotNull { it.shortName?.asString() }

        if ("Composable" !in annotations) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    koinViewModel() called outside @Composable → Runtime crash, requires Composition context
                    → Add @Composable annotation to the function

                    ✗ Bad:  fun MyScreen() { val vm = koinViewModel<MyVM>() }
                    ✓ Good: @Composable fun MyScreen() { val vm = koinViewModel<MyVM>() }
                    """.trimIndent()
                )
            )
        }
    }
}
