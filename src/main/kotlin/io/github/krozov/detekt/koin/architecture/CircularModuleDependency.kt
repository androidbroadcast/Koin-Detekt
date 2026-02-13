package io.github.krozov.detekt.koin.architecture

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

/**
 * Detects circular dependencies between Koin modules via `includes()`.
 *
 * Circular module dependencies cause initialization issues and make
 * dependency graph hard to understand.
 *
 * <noncompliant>
 * val moduleA = module {
 *     includes(moduleB)
 * }
 *
 * val moduleB = module {
 *     includes(moduleA) // Circular!
 * }
 * </noncompliant>
 *
 * <compliant>
 * val coreModule = module { }
 * val featureModule = module {
 *     includes(coreModule)
 * }
 * </compliant>
 */
public class CircularModuleDependency(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "CircularModuleDependency",
        severity = Severity.Warning,
        description = "Circular dependency between Koin modules",
        debt = Debt.TWENTY_MINS
    )

    private data class ModuleInfo(
        val name: String,
        val property: KtProperty,
        val dependencies: Set<String>
    )

    private val modules = mutableListOf<ModuleInfo>()

    override fun visitKtFile(file: KtFile) {
        modules.clear()
        super.visitKtFile(file)
        checkForCycles()
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)

        val moduleName = property.name ?: return
        val initializer = property.initializer as? KtCallExpression ?: return

        if (initializer.calleeExpression?.text == "module") {
            val includes = findIncludes(initializer)
            modules.add(ModuleInfo(moduleName, property, includes))
        }
    }

    private fun findIncludes(moduleCall: KtCallExpression): Set<String> {
        val includes = mutableSetOf<String>()

        moduleCall.accept(object : KtTreeVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)

                if (expression.calleeExpression?.text == "includes") {
                    expression.valueArguments.forEach { arg ->
                        arg.getArgumentExpression()?.text?.let { includes.add(it) }
                    }
                }
            }
        })

        return includes
    }

    private fun checkForCycles() {
        modules.forEach { module ->
            // Check for self-reference
            if (module.dependencies.contains(module.name)) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(module.property),
                        """
                        Module includes itself → Causes initialization errors
                        → Remove self-reference from includes()

                        ✗ Bad:  val ${module.name} = module { includes(${module.name}) }
                        ✓ Good: val ${module.name} = module { /* no self-reference */ }
                        """.trimIndent()
                    )
                )
                return@forEach // Skip circular dependency check for self-reference
            }

            // Check for direct circular dependency
            module.dependencies.forEach { depName ->
                val dependency = modules.find { it.name == depName }
                if (dependency != null && dependency.dependencies.contains(module.name)) {
                    report(
                        CodeSmell(
                            issue,
                            Entity.from(module.property),
                            """
                            Circular dependency: '${module.name}' ↔ '$depName' → Causes initialization errors
                            → Refactor to hierarchical structure (A → B, not A ↔ B)

                            ✗ Bad:  val ${module.name} = module { includes($depName) }; val $depName = module { includes(${module.name}) }
                            ✓ Good: val coreModule = module { }; val featureModule = module { includes(coreModule) }
                            """.trimIndent()
                        )
                    )
                }
            }
        }
    }
}
