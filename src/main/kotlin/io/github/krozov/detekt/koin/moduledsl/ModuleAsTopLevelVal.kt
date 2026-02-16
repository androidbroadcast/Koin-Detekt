package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Detects Koin modules defined as top-level `val` instead of functions.
 *
 * Defining a module as `val myModule = module {}` causes all factory lambdas to be preallocated
 * at module initialization time, even if they're never used. This wastes memory and increases
 * startup time. Using a function `fun myModule() = module {}` defers module creation until needed.
 *
 * Best Practice: Always define modules as functions, not vals.
 *
 * <noncompliant>
 * val appModule = module {
 *     single { Service() }
 *     factory { Repository() }
 * }
 * </noncompliant>
 *
 * <compliant>
 * fun appModule() = module {
 *     single { Service() }
 *     factory { Repository() }
 * }
 * </compliant>
 */
public class ModuleAsTopLevelVal(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "ModuleAsTopLevelVal",
        severity = Severity.CodeSmell,
        description = "Module defined as top-level val causes factory preallocation issues",
        debt = Debt.FIVE_MINS
    )

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)

        // Check if top-level val (not inside a class/object)
        if (!property.isTopLevel) return
        if (property.isVar) return

        // Check if initializer is module { }
        val initializer = property.initializer as? KtCallExpression ?: return
        if (initializer.calleeExpression?.text != "module") return

        report(
            CodeSmell(
                issue,
                Entity.from(property),
                """
                Module as top-level val → Factory preallocation issues
                → Use function instead: fun ${property.name}() = module {}

                ✗ Bad:  val ${property.name} = module { }
                ✓ Good: fun ${property.name}() = module { }
                """.trimIndent()
            )
        )
    }
}
