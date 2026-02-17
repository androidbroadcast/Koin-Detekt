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
 * `@Scoped` without `@Scope` annotation or scope archetype.
 *
 * Without a scope annotation, the scope boundary is undefined at runtime.
 *
 * <noncompliant>
 * @Scoped
 * class MyService // No scope defined!
 * </noncompliant>
 *
 * <compliant>
 * @Scope(name = "myScope")
 * @Scoped
 * class MyService
 * </compliant>
 */
public class ScopedWithoutQualifier(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "ScopedWithoutQualifier",
        severity = Severity.Warning,
        description = "@Scoped should specify scope name for clarity",
        debt = Debt.FIVE_MINS
    )

    private val scopeArchetypes = setOf(
        "Scope", "ViewModelScope", "ActivityScope", "ActivityRetainedScope", "FragmentScope"
    )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val annotations = klass.annotationEntries.mapNotNull { it.shortName?.asString() }
        val hasScopedAnnotation = "Scoped" in annotations
        if (!hasScopedAnnotation) return

        val hasScopeAnnotation = annotations.any { it in scopeArchetypes }

        if (!hasScopeAnnotation) {
            val scopedAnnotation = klass.annotationEntries.find {
                it.shortName?.asString() == "Scoped"
            } ?: return
            report(
                CodeSmell(
                    issue,
                    Entity.from(scopedAnnotation),
                    """
                    @Scoped without @Scope annotation → Undefined scope boundary at runtime
                    → Add @Scope, @ActivityScope, @FragmentScope, or similar to define scope

                    ✗ Bad:  @Scoped class ${klass.name}
                    ✓ Good: @Scope(name = "myScope") @Scoped class ${klass.name}
                    """.trimIndent()
                )
            )
        }
    }
}
