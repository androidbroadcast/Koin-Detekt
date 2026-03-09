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
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
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
        if (fqns.isNotEmpty() && fqns.none { it.startsWith("org.koin.") }) return

        // Get type reference from lambda body or constructor reference
        val isViewModel = when {
            // Pattern: single { MyViewModel() } or single<MyViewModel> { ... }
            callName == "single" -> {
                // Heuristic 1: type argument ends in "ViewModel" (e.g. single<MyViewModel> { ... })
                val typeArgText = expression.typeArgumentList?.arguments?.firstOrNull()?.text
                if (typeArgText != null && typeArgText.endsWith("ViewModel")) {
                    true
                } else {
                    val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()
                        ?: expression.valueArguments.firstOrNull()?.getArgumentExpression() as? KtLambdaExpression
                        ?: return
                    checkLambdaReturnsViewModel(lambda)
                }
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
        val body = lambda.bodyExpression ?: return false
        // Heuristic: any call expression in the lambda body whose callee ends with "ViewModel"
        // covers: MyViewModel(), MyViewModel(get()), MyViewModel(get(), get()), etc.
        return body.collectDescendantsOfType<KtCallExpression>().any { call ->
            val callee = call.calleeExpression?.text ?: return@any false
            callee.endsWith("ViewModel")
        }
    }

    private fun checkConstructorRefIsViewModel(expression: KtCallExpression): Boolean {
        // Heuristic: constructor ref class name ends with "ViewModel" (e.g. ::MyViewModel)
        // Using endsWith (not contains) avoids false positives like ::ViewModelFactory
        val argText = expression.valueArguments.firstOrNull()?.text ?: return false
        return argText.removePrefix("::").endsWith("ViewModel")
    }
}
