package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

internal class CloseableWithoutOnClose(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "CloseableWithoutOnClose",
        severity = Severity.Warning,
        description = "Detects Closeable/AutoCloseable types defined in single/scoped blocks " +
                "without onClose callback. This can lead to resource leaks.",
        debt = Debt.TEN_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.getCallNameExpression()?.text ?: return
        if (callName !in setOf("single", "scoped")) return

        // Check if lambda body creates Closeable (heuristic)
        val lambda = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()
            ?: expression.valueArguments.firstOrNull()?.getArgumentExpression() as? org.jetbrains.kotlin.psi.KtLambdaExpression
            ?: return

        val bodyText = lambda.bodyExpression?.text ?: return
        val isCloseable = bodyText.contains("Closeable") ||
                         bodyText.contains("Connection") ||
                         bodyText.contains("Stream")

        if (!isCloseable) return

        // Check if there's an onClose call after this definition
        val parent = expression.parent as? KtDotQualifiedExpression
        val hasOnClose = parent?.selectorExpression?.text?.contains("onClose") == true

        if (!hasOnClose) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    Closeable/AutoCloseable without onClose → Resource leak when scope closes
                    → Add onClose { it?.close() } to clean up resources

                    ✗ Bad:  single { DatabaseConnection() }
                    ✓ Good: single { DatabaseConnection() } onClose { it?.close() }
                    """.trimIndent()
                )
            )
        }
    }
}
