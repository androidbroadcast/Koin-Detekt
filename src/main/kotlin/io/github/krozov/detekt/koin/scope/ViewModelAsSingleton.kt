package io.github.krozov.detekt.koin.scope

import io.github.krozov.detekt.koin.util.ImportAwareRule
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

internal class ViewModelAsSingleton(config: Config) : ImportAwareRule(config) {

    override val issue: Issue = Issue(
        id = "ViewModelAsSingleton",
        severity = Severity.Warning,
        description = "Detects ViewModel classes defined with single {} instead of viewModel {}. " +
                "ViewModels defined as singletons can cause coroutine failures after navigation.",
        debt = Debt.TEN_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.getCallNameExpression()?.text ?: return
        if (callName !in setOf("single", "singleOf")) return
        // Guard: skip if the name resolves to a confirmed non-Koin FQN.
        // Use resolveFqn directly since org.koin.core.module.dsl is not in KOIN_PACKAGES.
        val fqns = importContext.resolveFqn(callName)
        if (fqns.isNotEmpty() && fqns.none { it.startsWith("org.koin") }) return

        // Get type reference from lambda body or constructor reference
        val isViewModel = when {
            // Pattern: single { MyViewModel() }
            callName == "single" -> {
                val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()
                    ?: expression.valueArguments.firstOrNull()?.getArgumentExpression() as? KtLambdaExpression
                    ?: return
                checkLambdaReturnsViewModel(lambda)
            }
            // Pattern: singleOf(::MyViewModel)
            callName == "singleOf" -> {
                checkConstructorRefIsViewModel(expression)
            }
            else -> false
        }

        if (isViewModel) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    ViewModel defined with single {} → ViewModelScope becomes invalid after popBackStack
                    → Use viewModel {} or viewModelOf() instead

                    ✗ Bad:  single { MyViewModel() }
                    ✓ Good: viewModel { MyViewModel() }
                    """.trimIndent()
                )
            )
        }
    }

    private fun checkLambdaReturnsViewModel(lambda: KtLambdaExpression): Boolean {
        // Simple heuristic: check if lambda body contains "ViewModel()" call
        val bodyText = lambda.bodyExpression?.text ?: return false
        return bodyText.contains("ViewModel()")
    }

    private fun checkConstructorRefIsViewModel(expression: KtCallExpression): Boolean {
        // Simple heuristic: check if argument contains "::.*ViewModel"
        val argText = expression.valueArguments.firstOrNull()?.text ?: return false
        return argText.contains("ViewModel")
    }
}
