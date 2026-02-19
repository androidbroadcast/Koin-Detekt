package io.github.krozov.detekt.koin.architecture

import io.github.krozov.detekt.koin.config.ConfigValidator
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * Restricts platform-specific Koin imports to appropriate modules.
 *
 * Configure import restrictions per platform:
 * ```yaml
 * PlatformImportRestriction:
 *   active: true
 *   restrictions:
 *     - import: 'org.koin.android.*'
 *       allowedPackages: ['com.example.app']
 *     - import: 'org.koin.compose.*'
 *       allowedPackages: ['com.example.ui']
 * ```
 *
 * <noncompliant>
 * package com.example.shared
 * import org.koin.android.ext.koin.androidContext // Wrong!
 * </noncompliant>
 *
 * <compliant>
 * package com.example.app
 * import org.koin.android.ext.koin.androidContext
 * </compliant>
 */
public class PlatformImportRestriction(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "PlatformImportRestriction",
        severity = Severity.Warning,
        description = "Platform-specific imports must be in appropriate modules",
        debt = Debt.TEN_MINS
    )

    private data class ImportRestriction(
        val importPattern: String,
        val allowedPackages: List<String>
    )

    private val restrictions: List<ImportRestriction> by lazy {
        val rawValue = valueOrNull<Any>("restrictions")
        val validation = ConfigValidator.validateList(
            configKey = "restrictions",
            value = rawValue,
            required = false,
            warnIfEmpty = true
        )

        if (!validation.isValid) {
            // Invalid configuration - fall back to empty list (rule will be inactive)
            return@lazy emptyList()
        }

        // Note: validation.warnings would contain empty list warning
        // but we can't log at this level in Detekt rules

        try {
            val rawRestrictions = rawValue as? List<*> ?: emptyList<Any>()
            rawRestrictions.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                ImportRestriction(
                    importPattern = map["import"] as? String ?: "",
                    allowedPackages = (map["allowedPackages"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                )
            }
        } catch (e: Exception) {
            // Failed to parse restrictions config - fall back to empty list
            emptyList()
        }
    }

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        if (restrictions.isEmpty()) return

        val packageName = file.packageFqName.asString()

        file.importDirectives.forEach { directive ->
            checkImport(directive, packageName)
        }
    }

    private fun checkImport(directive: KtImportDirective, packageName: String) {
        val importPath = directive.importPath?.pathStr ?: return

        restrictions.forEach { restriction ->
            if (matchesPattern(importPath, restriction.importPattern)) {
                val isAllowed = restriction.allowedPackages.any {
                    packageName == it || packageName.startsWith("$it.")
                }
                if (!isAllowed) {
                    report(
                        CodeSmell(
                            issue,
                            Entity.from(directive),
                            """
                            Platform-specific import in wrong package → Breaks multiplatform architecture
                            → Move to appropriate platform module: ${restriction.allowedPackages.joinToString(", ")}

                            ✗ Bad:  package $packageName; import $importPath
                            ✓ Good: package ${restriction.allowedPackages.firstOrNull() ?: "platform.module"}; import $importPath
                            """.trimIndent()
                        )
                    )
                }
            }
        }
    }

    private fun matchesPattern(importPath: String, pattern: String): Boolean {
        if (pattern.endsWith(".*")) {
            val prefix = pattern.dropLast(2)
            return importPath == prefix || importPath.startsWith("$prefix.")
        }
        return importPath == pattern
    }
}
