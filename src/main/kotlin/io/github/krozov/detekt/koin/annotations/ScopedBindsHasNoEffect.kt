package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass

/**
 * Detects `@Scoped(binds = [...])` which has no effect.
 *
 * Unlike `@Single` and `@Factory`, `@Scoped` does not support the `binds` parameter.
 * Koin Annotations silently ignores `binds` on `@Scoped`, so the binding is never registered.
 *
 * <noncompliant>
 * @Scoped(binds = [MyInterface::class])
 * class MyService : MyInterface
 * </noncompliant>
 *
 * <compliant>
 * @Single(binds = [MyInterface::class])
 * class MyService : MyInterface
 *
 * // Or use @Scoped without binds:
 * @Scoped
 * class MyService : MyInterface
 * </compliant>
 */
public class ScopedBindsHasNoEffect(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "ScopedBindsHasNoEffect",
        severity = Severity.Warning,
        description = "@Scoped(binds = [...]) has no effect — @Scoped does not support the binds parameter",
        debt = Debt.FIVE_MINS
    )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val scopedAnnotation = klass.annotationEntries
            .find { it.shortName?.asString() == "Scoped" } ?: return

        val hasBindsParam = scopedAnnotation.valueArgumentList?.arguments
            ?.any { it.getArgumentName()?.asName?.asString() == "binds" } == true

        if (hasBindsParam) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(scopedAnnotation),
                    """
                    @Scoped(binds = [...]) has no effect — @Scoped does not support the binds parameter
                    → Koin Annotations silently ignores binds= on @Scoped
                    → Use @Single(binds = [...]) if you need interface binding, or remove binds=

                    ✗ Bad:  @Scoped(binds = [MyInterface::class]) class ${klass.name ?: "MyClass"} : MyInterface
                    ✓ Good: @Single(binds = [MyInterface::class]) class ${klass.name ?: "MyClass"} : MyInterface
                    """.trimIndent()
                )
            )
        }
    }
}
