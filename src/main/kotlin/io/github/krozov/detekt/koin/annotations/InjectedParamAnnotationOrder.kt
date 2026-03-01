package io.github.krozov.detekt.koin.annotations

import io.github.krozov.detekt.koin.util.ImportAwareRule
import io.github.krozov.detekt.koin.util.hasKoinAnnotationFrom
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter

/**
 * Detects constructor parameters where `@InjectedParam` is not the first annotation.
 *
 * Due to a KSP code generator issue (InsertKoinIO/koin-annotations#315), placing any annotation
 * before `@InjectedParam` on a constructor parameter causes a compilation failure in the
 * generated code. `@InjectedParam` must always be the first annotation on the parameter.
 *
 * <noncompliant>
 * @Single
 * class MyService(
 *     @Suppress("Unused") @InjectedParam val name: String  // ❌ Compilation failure
 * )
 * </noncompliant>
 *
 * <compliant>
 * @Single
 * class MyService(
 *     @InjectedParam @Suppress("Unused") val name: String  // ✓ InjectedParam is first
 * )
 * </compliant>
 */
internal class InjectedParamAnnotationOrder(config: Config = Config.empty) : ImportAwareRule(config) {
    override val issue: Issue = Issue(
        id = "InjectedParamAnnotationOrder",
        severity = Severity.Warning,
        description = "@InjectedParam must be the first annotation on a constructor parameter",
        debt = Debt.FIVE_MINS
    )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        if (!klass.hasKoinAnnotationFrom(importContext, KoinAnnotationConstants.DEFINITION_ANNOTATIONS)) return

        klass.primaryConstructor?.valueParameters?.forEach { param ->
            checkParameter(param)
        }
    }

    private fun checkParameter(param: KtParameter) {
        val annotations = param.annotationEntries.mapNotNull { it.shortName?.asString() }
        val injectedParamIndex = annotations.indexOf("InjectedParam")

        if (injectedParamIndex > 0) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(param),
                    """
                    @InjectedParam is not the first annotation on parameter '${param.name}'
                    → KSP code generator requires @InjectedParam to be the first annotation
                    → Related issue: InsertKoinIO/koin-annotations#315

                    ✗ Bad:  @Suppress("Unused") @InjectedParam val ${param.name}: ${param.typeReference?.text}
                    ✓ Good: @InjectedParam @Suppress("Unused") val ${param.name}: ${param.typeReference?.text}
                    """.trimIndent()
                )
            )
        }
    }
}
