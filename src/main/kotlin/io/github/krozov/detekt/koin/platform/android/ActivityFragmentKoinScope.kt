package io.github.krozov.detekt.koin.platform.android

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Detects misuse of `activityScope()` / `fragmentScope()`.
 *
 * Scopes must match component lifecycle. Using wrong scope
 * causes memory leaks or crashes.
 *
 * <noncompliant>
 * class MyFragment : Fragment() {
 *     val vm by activityScope().inject<VM>() // Wrong scope!
 * }
 * </noncompliant>
 *
 * <compliant>
 * class MyFragment : Fragment() {
 *     val vm by fragmentScope().inject<VM>()
 * }
 * </compliant>
 */
public class ActivityFragmentKoinScope(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "ActivityFragmentKoinScope",
        severity = Severity.Warning,
        description = "activityScope/fragmentScope must match component lifecycle",
        debt = Debt.TEN_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.calleeExpression?.text ?: return
        if (callName != "activityScope" && callName != "fragmentScope") return

        val containingClass = expression.getStrictParentOfType<KtClass>() ?: return
        val superTypes = containingClass.getSuperTypeListEntries()
            .mapNotNull { it.typeAsUserType?.referencedName }

        val isFragment = superTypes.any { it.contains("Fragment") }
        val isActivity = superTypes.any { it.contains("Activity") }

        if (isFragment && callName == "activityScope") {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    activityScope() in Fragment → Scope outlives Fragment, memory leaks/crashes
                    → Use fragmentScope() to match Fragment lifecycle

                    ✗ Bad:  class MyFragment : Fragment() { val vm by activityScope().inject<VM>() }
                    ✓ Good: class MyFragment : Fragment() { val vm by fragmentScope().inject<VM>() }
                    """.trimIndent()
                )
            )
        } else if (isActivity && callName == "fragmentScope") {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    fragmentScope() in Activity → Wrong lifecycle scope, potential crashes
                    → Use activityScope() to match Activity lifecycle

                    ✗ Bad:  class MyActivity : Activity() { val vm by fragmentScope().inject<VM>() }
                    ✓ Good: class MyActivity : Activity() { val vm by activityScope().inject<VM>() }
                    """.trimIndent()
                )
            )
        }
    }
}
