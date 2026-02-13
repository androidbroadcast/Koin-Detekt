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
 * Detects `koinInject()` calls in `@Preview` functions.
 *
 * Compose Previews run without Koin context, causing crashes when trying to inject dependencies.
 * Use fake/mock implementations or preview parameters instead.
 *
 * <noncompliant>
 * @Preview
 * @Composable
 * fun MyScreenPreview() {
 *     val repo = koinInject<Repository>() // Preview crash!
 *     MyScreen(repo)
 * }
 * </noncompliant>
 *
 * <compliant>
 * @Preview
 * @Composable
 * fun MyScreenPreview() {
 *     MyScreen(FakeRepository())
 * }
 * </compliant>
 */
public class KoinInjectInPreview(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "KoinInjectInPreview",
        severity = Severity.Warning,
        description = "koinInject() should not be used in @Preview functions",
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.calleeExpression?.text ?: return
        if (callName != "koinInject") return

        val containingFunction = expression.getStrictParentOfType<KtNamedFunction>() ?: return
        val annotations = containingFunction.annotationEntries.mapNotNull { it.shortName?.asString() }

        if ("Preview" in annotations) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    koinInject() in @Preview function → Preview crash, no Koin context available
                    → Use fake/mock implementations or @Preview parameters instead

                    ✗ Bad:  @Preview @Composable fun MyPreview() { val repo = koinInject<Repo>() }
                    ✓ Good: @Preview @Composable fun MyPreview() { MyScreen(FakeRepo()) }
                    """.trimIndent()
                )
            )
        }
    }
}
