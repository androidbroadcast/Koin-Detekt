package io.github.krozov.detekt.koin.util

import org.jetbrains.kotlin.psi.KtFile

/**
 * Resolves short annotation/type names to fully-qualified names using the import directives of a
 * Kotlin source file.
 *
 * Resolution order for [resolveFqn]:
 * 1. If [name] already contains '.' it is treated as an FQN and returned as-is.
 * 2. Exact or alias imports are consulted — a match returns a single-element set.
 * 3. If the file has a non-empty package and no explicit imports at all,
 *    a same-package candidate is returned
 * 4. Otherwise an empty set is returned.
 *
 * Star imports are NOT expanded here — use [hasStarImportFrom] to check for wildcard presence.
 */
internal class FileImportContext private constructor(
    private val exactImports: Map<String, String>,  // localName -> FQN
    private val starPackages: Set<String>,           // "org.koin.core.annotation" (without .*)
    val filePackage: String,
) {

    /** Returns `true` if the file contains at least one star import (from any package). */
    val hasAnyStarImport: Boolean get() = starPackages.isNotEmpty()

    /**
     * Returns all candidate FQNs for [name].
     *
     * See class-level KDoc for the resolution order.
     */
    fun resolveFqn(name: String): Set<String> {
        if (name.isEmpty()) return emptySet()
        if (name.contains('.')) return setOf(name)

        val exact = exactImports[name]
        if (exact != null) return setOf(exact)

        // Same-package candidate: only when the file has no explicit imports at all.
        // If the file has imports, an unresolved name is treated as "not found" rather than
        // assumed to come from the same package.
        if (filePackage.isNotEmpty() && exactImports.isEmpty()) return setOf("$filePackage.$name")

        return emptySet()
    }

    /** Returns `true` if the file contains a star-import from [packagePrefix] or any sub-package. */
    fun hasStarImportFrom(packagePrefix: String): Boolean =
        starPackages.any { it == packagePrefix || it.startsWith("$packagePrefix.") }

    companion object {
        /** Sentinel value for contexts where no source file is available. */
        val EMPTY: FileImportContext = FileImportContext(
            exactImports = emptyMap(),
            starPackages = emptySet(),
            filePackage = "",
        )

        /** Builds a [FileImportContext] by parsing the import directives of [file]. */
        operator fun invoke(file: KtFile): FileImportContext {
            val pkg = file.packageFqName.asString()
            val exact = mutableMapOf<String, String>()
            val stars = mutableSetOf<String>()

            for (directive in file.importDirectives) {
                val fqn = directive.importedFqName?.asString() ?: continue
                val alias = directive.alias?.name

                if (directive.isAllUnder) {
                    // Star import: fqn is the package prefix (without the .*)
                    stars.add(fqn)
                } else {
                    val localName = alias ?: fqn.substringAfterLast('.')
                    exact[localName] = fqn
                }
            }

            return FileImportContext(
                exactImports = exact,
                starPackages = stars,
                filePackage = pkg,
            )
        }
    }
}
