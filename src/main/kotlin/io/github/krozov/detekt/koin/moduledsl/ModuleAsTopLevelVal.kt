package io.github.krozov.detekt.koin.moduledsl

import io.github.krozov.detekt.koin.util.ImportAwareRule
import io.github.krozov.detekt.koin.util.Resolution
import io.github.krozov.detekt.koin.util.resolveKoin
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Detects Koin modules defined as top-level `val` instead of functions.
 *
 * Defining a module as `val myModule = module {}` initializes the module eagerly via JVM class
 * loading, which makes it impossible to substitute or replace the module in tests. Using a
 * function `fun myModule() = module {}` allows test code to call a different factory function,
 * making the module swappable and the code properly testable.
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
internal class ModuleAsTopLevelVal(config: Config = Config.empty) : ImportAwareRule(config) {
    override val issue: Issue = Issue(
        id = "ModuleAsTopLevelVal",
        severity = Severity.CodeSmell,
        description = "Module defined as top-level val cannot be swapped in tests — use a function instead",
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
        if (importContext.resolveKoin("module") == Resolution.NOT_KOIN) return

        report(
            CodeSmell(
                issue,
                Entity.from(property),
                """
                Module as top-level val → Cannot be swapped in tests
                → Use function instead: fun ${property.name}() = module {}

                ✗ Bad:  val ${property.name} = module { }
                ✓ Good: fun ${property.name}() = module { }
                """.trimIndent()
            )
        )
    }
}
