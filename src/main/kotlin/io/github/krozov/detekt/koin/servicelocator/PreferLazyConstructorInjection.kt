package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.github.krozov.detekt.koin.util.value
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

internal class PreferLazyConstructorInjection(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "PreferLazyConstructorInjection",
        severity = Severity.Warning,
        description = "Constructor parameter type should be wrapped in Lazy<T> for deferred resolution.",
        debt = Debt.FIVE_MINS
    )

    private val checkAllTypes: Boolean =
        config.value(key = "checkAllTypes", default = false)

    private val lazyTypes: List<String> =
        config.value(key = "lazyTypes", default = emptyList())

    private val excludeTypes: List<String> =
        config.value(key = "excludeTypes", default = emptyList())

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
        super.visitPrimaryConstructor(constructor)

        if (!checkAllTypes && lazyTypes.isEmpty()) return

        val file = constructor.containingKtFile

        constructor.valueParameters.forEach { param ->
            val typeRef = param.typeReference ?: return@forEach
            val rawType = typeRef.text.trim()

            if (param.isVarArg) return@forEach
            if (rawType.startsWith("Lazy<")) return@forEach
            if (rawType.startsWith("(")) return@forEach

            val isNullable = rawType.endsWith("?")
            val shortName = rawType.removeSuffix("?").trim()
            val lazyType = if (isNullable) "Lazy<$shortName?>" else "Lazy<$shortName>"
            val resolvedFqn = resolveTypeFqn(shortName, file)

            if (isExcluded(resolvedFqn, shortName)) return@forEach
            if (!shouldFlag(resolvedFqn, shortName)) return@forEach

            report(
                CodeSmell(
                    issue,
                    Entity.from(param),
                    """
                    $shortName injected eagerly → consider $lazyType for deferred resolution

                    ✗ Current:  ${param.text}
                    ✓ Preferred: ${param.text.replace(rawType, lazyType)}

                    Then access via: ${param.name}.value
                    Note: for Koin DSL modules, the compiler will guide you to update get() → inject()
                    """.trimIndent()
                )
            )
        }
    }

    private fun resolveTypeFqn(shortName: String, file: KtFile): String? {
        if (shortName.contains('.')) return shortName
        return file.importDirectives
            .mapNotNull { it.importedFqName?.asString() }
            .firstOrNull { it.substringAfterLast('.') == shortName }
    }

    private fun isExcluded(fqn: String?, shortName: String): Boolean =
        excludeTypes.any { entry -> matchesEntry(entry, fqn, shortName) }

    private fun shouldFlag(fqn: String?, shortName: String): Boolean {
        if (checkAllTypes) return true
        return lazyTypes.any { entry -> matchesEntry(entry, fqn, shortName) }
    }

    private fun matchesEntry(entry: String, fqn: String?, shortName: String): Boolean {
        val baseShortName = shortName.substringBefore("<")
        return if (entry.contains('.')) {
            val entryShortName = entry.substringAfterLast('.')
            fqn == entry || (fqn == null && (shortName == entryShortName || baseShortName == entryShortName))
        } else {
            shortName == entry || baseShortName == entry
        }
    }
}
