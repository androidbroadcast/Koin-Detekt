package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Detects potential override issues in modules that use `includes()`.
 *
 * When a module includes another module via `includes()`, attempting to override definitions
 * from the included module doesn't work as expected. Koin will raise a `DefinitionOverrideException`
 * unless `override = true` is explicitly specified.
 *
 * This rule warns when finding duplicate type registrations across modules when one uses `includes()`.
 *
 * **Note:** This is a simplified heuristic-based detection. Complex cross-module analysis may require
 * semantic type resolution beyond PSI capabilities.
 *
 * Reference: https://github.com/InsertKoinIO/koin/issues/1919
 *
 * <noncompliant>
 * val baseModule = module {
 *     single<Service> { ServiceA() }
 * }
 *
 * val overrideModule = module {
 *     includes(baseModule)
 *     single<Service> { ServiceB() } // ❌ DefinitionOverrideException at runtime
 * }
 * </noncompliant>
 *
 * <compliant>
 * val baseModule = module {
 *     single<Service> { ServiceA() }
 * }
 *
 * val overrideModule = module {
 *     includes(baseModule)
 *     single<Service>(override = true) { ServiceB() } // ✓ Explicit override
 * }
 * </compliant>
 */
public class OverrideInIncludedModule(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "OverrideInIncludedModule",
        severity = Severity.Warning,
        description = "Overriding definitions from included modules requires explicit override = true",
        debt = Debt.FIVE_MINS
    )

    private data class TypeRegistration(val type: String, val call: KtCallExpression, val hasOverride: Boolean)

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        // Collect all module calls in the file
        val moduleCalls = file.collectDescendantsOfType<KtCallExpression>().filter {
            it.calleeExpression?.text == "module"
        }

        // For each module, check if it has includes() and duplicate type registrations
        moduleCalls.forEach { moduleCall ->
            checkModuleForDuplicates(moduleCall, file)
        }
    }

    private fun checkModuleForDuplicates(moduleCall: KtCallExpression, file: KtFile) {
        val lambda = moduleCall.lambdaArguments.firstOrNull()?.getLambdaExpression()
            ?: moduleCall.valueArguments.firstOrNull()?.getArgumentExpression() as? KtLambdaExpression
            ?: return

        val bodyExpression = lambda.bodyExpression ?: return

        // Check if this module has includes()
        val hasIncludes = bodyExpression.collectDescendantsOfType<KtCallExpression>().any {
            it.calleeExpression?.text == "includes"
        }

        if (!hasIncludes) return

        // Collect all type registrations in this module
        val typesInThisModule = mutableListOf<TypeRegistration>()

        bodyExpression.collectDescendantsOfType<KtCallExpression>().forEach { call ->
            val callName = call.calleeExpression?.text
            if (callName in setOf("single", "factory", "scoped")) {
                val hasOverride = call.valueArguments.any { arg ->
                    val argName = arg.getArgumentName()?.asName?.asString()
                    argName == "override" && arg.getArgumentExpression()?.text == "true"
                }

                // Get type from type arguments
                val typeArgs = call.typeArgumentList?.arguments
                val registeredType = typeArgs?.firstOrNull()?.text

                if (registeredType != null && !hasOverride) {
                    typesInThisModule.add(TypeRegistration(registeredType, call, hasOverride))
                }

                // Check for bind syntax
                val parent = call.parent
                if (parent is KtBinaryExpression && parent.operationReference.text == "bind") {
                    val boundType = parent.right?.text?.replace("::class", "")?.trim()
                    if (boundType != null && !hasOverride) {
                        typesInThisModule.add(TypeRegistration(boundType, call, hasOverride))
                    }
                }
            }
        }

        // Collect all type registrations in the entire file
        val allTypes = file.collectDescendantsOfType<KtCallExpression>().mapNotNull { call ->
            val callName = call.calleeExpression?.text
            if (callName in setOf("single", "factory", "scoped")) {
                val typeArgs = call.typeArgumentList?.arguments
                typeArgs?.firstOrNull()?.text
            } else null
        }

        // Also collect bind types
        val allBindTypes = file.collectDescendantsOfType<KtBinaryExpression>()
            .filter { it.operationReference.text == "bind" }
            .mapNotNull { it.right?.text?.replace("::class", "")?.trim() }

        val allTypesInFile = allTypes + allBindTypes

        // Check if any types in this module appear elsewhere in the file
        typesInThisModule.forEach { typeReg ->
            val occurrences = allTypesInFile.count { it == typeReg.type }
            if (occurrences > 1) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(typeReg.call),
                        """
                        Module with includes() defines type that may conflict: ${typeReg.type}
                        → Overriding definitions from included modules requires override = true
                        → Or use separate modules without includes()

                        ✗ Bad:  module { includes(base); single<${typeReg.type}> { ... } }
                        ✓ Good: module { includes(base); single<${typeReg.type}>(override = true) { ... } }
                        """.trimIndent()
                    )
                )
            }
        }
    }
}
