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
 * Detects classes with more than 5 `@InjectedParam` parameters.
 *
 * `ParametersHolder` only supports destructuring up to `component5()`.
 * Exceeding this limit causes a confusing compile error.
 *
 * <noncompliant>
 * @Single
 * class MyService(
 *     @InjectedParam val a: String,
 *     @InjectedParam val b: Int,
 *     @InjectedParam val c: Long,
 *     @InjectedParam val d: Float,
 *     @InjectedParam val e: Double,
 *     @InjectedParam val f: Boolean // 6th — too many!
 * )
 * </noncompliant>
 *
 * <compliant>
 * @Single
 * class MyService(
 *     @InjectedParam val params: MyServiceParams // Use wrapper
 * )
 * </compliant>
 */
public class TooManyInjectedParams(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "TooManyInjectedParams",
        severity = Severity.Warning,
        description = "Too many @InjectedParam parameters — ParametersHolder supports max 5",
        debt = Debt.TEN_MINS
    )

    @Suppress("MemberVisibilityCanBePrivate")
    internal val maxInjectedParams = valueOrDefault("maxInjectedParams", 5)

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val primaryConstructor = klass.primaryConstructor ?: return
        val injectedParamCount = primaryConstructor.valueParameters.count { param ->
            param.annotationEntries.any { it.shortName?.asString() == "InjectedParam" }
        }

        if (injectedParamCount > maxInjectedParams) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(klass),
                    """
                    Class has $injectedParamCount @InjectedParam parameters, but ParametersHolder supports max $maxInjectedParams
                    → Reduce parameters or use a wrapper class

                    ✗ Bad:  class ${klass.name}(@InjectedParam a, @InjectedParam b, ... $injectedParamCount params)
                    ✓ Good: class ${klass.name}(@InjectedParam params: ${klass.name}Params)
                    """.trimIndent()
                )
            )
        }
    }
}
