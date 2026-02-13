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
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

internal class MissingScopeClose(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "MissingScopeClose",
        severity = Severity.Warning,
        description = "Detects classes that create or obtain a Scope but never call scope.close(). " +
                "This can lead to memory leaks.",
        debt = Debt.TEN_MINS
    )

    // Thread-safe state using ThreadLocal to support parallel file processing
    private val classesWithScopeCreation = ThreadLocal.withInitial { mutableSetOf<KtClass>() }
    private val classesWithScopeClose = ThreadLocal.withInitial { mutableSetOf<KtClass>() }

    override fun visitKtFile(file: KtFile) {
        val scopeCreation = classesWithScopeCreation.get()
        val scopeClose = classesWithScopeClose.get()

        scopeCreation.clear()
        scopeClose.clear()

        super.visitKtFile(file)

        // Report all classes with scope creation but no close
        (scopeCreation - scopeClose).forEach { klass ->
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
        checkCloseCall(expression)
    }

    override fun visitSafeQualifiedExpression(expression: KtSafeQualifiedExpression) {
        super.visitSafeQualifiedExpression(expression)
        checkCloseCall(expression)
    }

    private fun checkCloseCall(expression: KtExpression) {
        val selectorCall = when (expression) {
            is KtDotQualifiedExpression -> expression.selectorExpression as? KtCallExpression
            is KtSafeQualifiedExpression -> expression.selectorExpression as? KtCallExpression
            else -> null
        }
        val callName = selectorCall?.getCallNameExpression()?.text

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
                containingClass?.let { classesWithScopeCreation.get().add(it) }
            }
            "close" -> {
                val receiverExpression = when (expression) {
                    is KtDotQualifiedExpression -> expression.receiverExpression
                    is KtSafeQualifiedExpression -> expression.receiverExpression
                    else -> null
                }
                val receiverText = receiverExpression?.text ?: ""
                val receiverName = (receiverExpression as? KtNameReferenceExpression)?.getReferencedName()
                if (receiverName == "scope" || receiverText.endsWith(".scope")) {
                    containingClass?.let { classesWithScopeClose.get().add(it) }
                }
            }
        }
    }
}
