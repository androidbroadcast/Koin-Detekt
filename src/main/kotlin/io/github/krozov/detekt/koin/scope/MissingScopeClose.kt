package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

internal class MissingScopeClose(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "MissingScopeClose",
        severity = Severity.Warning,
        description = "Detects classes that create or obtain a Scope but never call scope.close(). " +
                "This can lead to memory leaks.",
        debt = Debt.TEN_MINS
    )

    private val classesWithScopeCreation = mutableSetOf<KtClass>()
    private val classesWithScopeClose = mutableSetOf<KtClass>()

    override fun visitKtFile(file: KtFile) {
        classesWithScopeCreation.clear()
        classesWithScopeClose.clear()

        super.visitKtFile(file)

        // Report all classes with scope creation but no close
        (classesWithScopeCreation - classesWithScopeClose).forEach { klass ->
            report(
                CodeSmell(
                    issue,
                    Entity.from(klass),
                    "Class '${klass.name}' creates a Scope but never calls close(). This may cause memory leaks."
                )
            )
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        val selectorCall = expression.selectorExpression as? KtCallExpression
        val callName = selectorCall?.getCallNameExpression()?.text

        // Find containing class by traversing parent hierarchy
        var parent = expression.parent
        var containingClass: KtClass? = null
        while (parent != null) {
            if (parent is KtClass) {
                containingClass = parent
                break
            }
            parent = parent.parent
        }

        when (callName) {
            "createScope", "getOrCreateScope" -> {
                containingClass?.let { classesWithScopeCreation.add(it) }
            }
            "close" -> {
                // Check if receiver is exactly 'scope' or ends with '.scope'
                val receiverText = expression.receiverExpression.text
                val receiverName = (expression.receiverExpression as? KtNameReferenceExpression)?.getReferencedName()
                if (receiverName == "scope" || receiverText.endsWith(".scope")) {
                    containingClass?.let { classesWithScopeClose.add(it) }
                }
            }
        }
    }
}
