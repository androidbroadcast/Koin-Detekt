package io.github.krozov.detekt.koin.platform.compose

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
 * Detects `loadKoinModules()` inside `remember {}` without corresponding unload.
 *
 * Memory leak - modules are loaded on every recomposition without cleanup.
 * Use DisposableEffect with unloadKoinModules instead.
 *
 * <noncompliant>
 * @Composable
 * fun FeatureScreen() {
 *     remember { loadKoinModules(featureModule) } // Memory leak!
 * }
 * </noncompliant>
 *
 * <compliant>
 * @Composable
 * fun FeatureScreen() {
 *     DisposableEffect(Unit) {
 *         loadKoinModules(featureModule)
 *         onDispose { unloadKoinModules(featureModule) }
 *     }
 * }
 * </compliant>
 */
public class RememberKoinModulesLeak(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "RememberKoinModulesLeak",
        severity = Severity.Warning,
        description = "loadKoinModules in remember without unload causes memory leak",
        debt = Debt.TEN_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.calleeExpression?.text ?: return
        if (callName != "loadKoinModules") return

        // Check if inside remember {} lambda
        val parentLambda = expression.getStrictParentOfType<KtLambdaExpression>() ?: return
        val rememberCall = parentLambda.parent?.parent as? KtCallExpression ?: return
        val rememberName = rememberCall.calleeExpression?.text ?: return

        if (rememberName == "remember") {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "loadKoinModules() inside remember {} without unloadKoinModules() causes memory leak. " +
                            "Use DisposableEffect with onDispose { unloadKoinModules() } instead."
                )
            )
        }
    }
}
