package io.github.krozov.detekt.koin.architecture

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

    private val restrictedLayers: List<String> = valueOrDefault("restrictedLayers", emptyList())
    private val allowedImports: List<String> = valueOrDefault("allowedImports", emptyList())

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
                    "Star import '$importPath.*' not allowed in restricted layer '$packageName'. " +
                            "Domain/Core layers should not depend on Koin."
                )
            )
            return
        }

        // Regular Koin import in restricted layer
        report(
            CodeSmell(
                issue,
                Entity.from(directive),
                "Import '$importPath' not allowed in restricted layer '$packageName'. " +
                        "Domain/Core layers should use constructor injection instead of Koin APIs."
            )
        )
    }
}
