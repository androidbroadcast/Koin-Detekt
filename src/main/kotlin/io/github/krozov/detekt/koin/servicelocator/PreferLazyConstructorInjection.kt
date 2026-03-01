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

/**
 * Detects constructor parameters whose type should be wrapped in `Lazy<T>` for deferred resolution.
 *
 * **Known limitation — star imports:** When a FQN entry (e.g. `com.example.Foo`) is configured and
 * the file uses a star import (`import com.example.*`), type resolution falls back to short-name
 * matching. This may produce false positives if another package also exports a class with the same
 * simple name.
 */
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
            // Full type text without trailing '?' — may include generic arguments, e.g. "Map<String, Int>"
            val typeText = rawType.removeSuffix("?").trim()
            // Outer type name without generics — used for import lookup and entry matching
            val baseName = typeText.substringBefore("<")
            val lazyType = if (isNullable) "Lazy<$typeText?>" else "Lazy<$typeText>"
            val resolvedFqn = resolveTypeFqn(baseName, file)

            if (isExcluded(resolvedFqn, typeText)) return@forEach
            if (!shouldFlag(resolvedFqn, typeText)) return@forEach

            report(
                CodeSmell(
                    issue,
                    Entity.from(param),
                    """
                    $typeText injected eagerly → consider $lazyType for deferred resolution

                    ✗ Current:  ${param.text}
                    ✓ Preferred: ${param.text.replace(rawType, lazyType)}

                    Then access via: ${param.name}.value
                    Note: for Koin DSL modules, the compiler will guide you to update get() → inject()
                    """.trimIndent()
                )
            )
        }
    }

    /**
     * Resolves [baseName] (outer type name, no generics) to a fully-qualified name via import
     * directives. Returns the FQN string if resolved, or `null` when unresolvable (e.g. star
     * import, no import at all). When [baseName] already contains a dot it is treated as inline FQN.
     *
     * **Known limitation — import aliases:** `import com.example.Foo as Bar` is not handled;
     * a parameter typed as `Bar` will not be matched against a `com.example.Foo` config entry.
     */
    private fun resolveTypeFqn(baseName: String, file: KtFile): String? {
        if (baseName.contains('.')) return baseName
        return file.importDirectives
            .mapNotNull { it.importedFqName?.asString() }
            .firstOrNull { it.substringAfterLast('.') == baseName }
    }

    // typeText — full type text without '?', may include generics, e.g. "Map<String, Int>"
    private fun isExcluded(fqn: String?, typeText: String): Boolean =
        excludeTypes.any { entry -> matchesEntry(entry, fqn, typeText) }

    private fun shouldFlag(fqn: String?, typeText: String): Boolean {
        if (checkAllTypes) return true
        return lazyTypes.any { entry -> matchesEntry(entry, fqn, typeText) }
    }

    private fun matchesEntry(entry: String, fqn: String?, typeText: String): Boolean {
        val baseName = typeText.substringBefore("<")
        val normalizedFqn = fqn?.substringBefore("<")
        return if (entry.contains('.')) {
            val entryShortName = entry.substringAfterLast('.')
            normalizedFqn == entry || (normalizedFqn == null && (typeText == entryShortName || baseName == entryShortName))
        } else {
            typeText == entry || baseName == entry
        }
    }
}
