package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeReference

/**
 * Detects `@InjectedParam` with nested generic types or star projections.
 *
 * KSP code generator has a known bug where nested type arguments are dropped,
 * e.g. `List<List<String>>` generates `List<kotlin.collections.List>`.
 *
 * <noncompliant>
 * @Single
 * class MyService(@InjectedParam val items: List<List<String>>) // KSP bug!
 * </noncompliant>
 *
 * <compliant>
 * typealias StringList = List<String>
 *
 * @Single
 * class MyService(@InjectedParam val items: List<StringList>) // Workaround
 * </compliant>
 */
public class InjectedParamWithNestedGenericType(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "InjectedParamWithNestedGenericType",
        severity = Severity.Warning,
        description = "@InjectedParam with nested generic type — KSP generates incorrect code",
        debt = Debt.TEN_MINS
    )

    override fun visitParameter(parameter: KtParameter) {
        super.visitParameter(parameter)

        val hasInjectedParam = parameter.annotationEntries.any {
            it.shortName?.asString() == "InjectedParam"
        }
        if (!hasInjectedParam) return

        val typeRef = parameter.typeReference ?: return

        if (hasNestedGenerics(typeRef) || hasStarProjection(typeRef)) {
            val typeText = typeRef.text
            report(
                CodeSmell(
                    issue,
                    Entity.from(parameter),
                    """
                    @InjectedParam with nested generic type '$typeText' — KSP may generate incorrect code
                    → Use a typealias or wrapper class to flatten the type

                    ✗ Bad:  @InjectedParam val param: $typeText
                    ✓ Good: typealias FlatType = ...; @InjectedParam val param: FlatType
                    """.trimIndent()
                )
            )
        }
    }

    private fun hasNestedGenerics(typeRef: KtTypeReference): Boolean {
        val typeElement = typeRef.typeElement ?: return false
        val typeArgList = typeElement.children.filterIsInstance<KtTypeArgumentList>().firstOrNull()
            ?: return false

        return typeArgList.arguments.any { projection ->
            val innerTypeRef = projection.typeReference ?: return@any false
            val innerTypeElement = innerTypeRef.typeElement ?: return@any false
            innerTypeElement.children.any { it is KtTypeArgumentList }
        }
    }

    private fun hasStarProjection(typeRef: KtTypeReference): Boolean {
        val typeElement = typeRef.typeElement ?: return false
        val typeArgList = typeElement.children.filterIsInstance<KtTypeArgumentList>().firstOrNull()
            ?: return false

        return typeArgList.arguments.any { projection ->
            projection.text.trim() == "*"
        }
    }
}
