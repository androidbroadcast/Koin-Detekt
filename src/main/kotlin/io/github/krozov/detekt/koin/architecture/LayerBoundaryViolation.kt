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
 * Enforces Clean Architecture by restricting Koin imports in specified layers.
 *
 * Configure restricted layers and optionally allowed imports:
 * ```yaml
 * LayerBoundaryViolation:
 *   active: true
 *   restrictedLayers:
 *     - 'com.example.domain'
 *     - 'com.example.core'
 *   allowedImports:
 *     - 'org.koin.core.qualifier.Qualifier'
 * ```
 *
 * <noncompliant>
 * package com.example.domain
 * import org.koin.core.component.get
 *
 * class UseCase {
 *     val repo = get<Repository>() // Violates Clean Architecture
 * }
 * </noncompliant>
 *
 * <compliant>
 * package com.example.domain
 *
 * class UseCase(
 *     private val repo: Repository // Constructor injection
 * )
 * </compliant>
 */
public class LayerBoundaryViolation(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "LayerBoundaryViolation",
        severity = Severity.Warning,
        description = "Koin imports not allowed in restricted architectural layers",
        debt = Debt.TWENTY_MINS
    )

    private val restrictedLayers: List<String> by lazy {
        val rawValue = config.valueOrNull<Any>("restrictedLayers")
        val validation = ConfigValidator.validateList(
            configKey = "restrictedLayers",
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

        (rawValue as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    }

    private val allowedImports: List<String> by lazy {
        val rawValue = config.valueOrNull<Any>("allowedImports")
        val validation = ConfigValidator.validateList(
            configKey = "allowedImports",
            value = rawValue,
            required = false,
            warnIfEmpty = false
        )

        if (!validation.isValid) {
            // Invalid configuration - fall back to empty list
            return@lazy emptyList()
        }

        (rawValue as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    }

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        if (restrictedLayers.isEmpty()) return

        val packageName = file.packageFqName.asString()
        val isRestricted = restrictedLayers.any { packageName.startsWith(it) }

        if (!isRestricted) return

        file.importDirectives.forEach { directive ->
            checkImport(directive, packageName)
        }
    }

    private fun checkImport(directive: KtImportDirective, packageName: String) {
        val importPath = directive.importPath?.pathStr ?: return

        // Check if it's a Koin import
        if (!importPath.startsWith("org.koin")) return

        // Check if it's in allowed list
        if (allowedImports.any { importPath == it || importPath.startsWith("$it.") }) return

        // Star imports are always violations in restricted layers
        if (directive.isAllUnder) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(directive),
                    """
                    Koin import in restricted layer '$packageName' → Violates Clean Architecture boundaries
                    → Domain/Core layers should be framework-agnostic and use constructor injection

                    ✗ Bad:  import $importPath.*
                    ✓ Good: class UseCase(private val repo: Repository)
                    """.trimIndent()
                )
            )
            return
        }

        // Regular Koin import in restricted layer
        report(
            CodeSmell(
                issue,
                Entity.from(directive),
                """
                Koin import '$importPath' in restricted layer → Violates Clean Architecture boundaries
                → Domain/Core layers should be framework-agnostic and use constructor injection

                ✗ Bad:  import $importPath
                ✓ Good: class UseCase(private val repo: Repository)
                """.trimIndent()
            )
        )
    }
}
