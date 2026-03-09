package io.github.krozov.detekt.koin.moduledsl

import io.github.krozov.detekt.koin.util.ImportAwareRule
import io.github.krozov.detekt.koin.util.Resolution
import io.github.krozov.detekt.koin.util.resolveKoin
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
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
internal class OverrideInIncludedModule(config: Config = Config.empty) : ImportAwareRule(config) {
    override val issue: Issue = Issue(
        id = "OverrideInIncludedModule",
        severity = Severity.Warning,
        description = "Overriding definitions from included modules requires explicit override = true",
        debt = Debt.FIVE_MINS
    )

    private data class TypeRegistration(val type: String, val call: KtCallExpression, val hasOverride: Boolean)

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        if (importContext.resolveKoin("module") == Resolution.NOT_KOIN) return

        // Build a map of top-level module variable/property name → its body expression
        // so we can look up types defined in included modules.
        val moduleBodyByName = buildModuleBodyMap(file)

        // Collect all module calls in the file
        val moduleCalls = file.collectDescendantsOfType<KtCallExpression>().filter {
            it.calleeExpression?.text == "module"
        }

        // For each module, check if it has includes() and conflicting type registrations
        moduleCalls.forEach { moduleCall ->
            checkModuleForDuplicates(moduleCall, moduleBodyByName)
        }
    }

    /**
     * Builds a map from property/variable name → the lambda body of the `module { ... }` call.
     * Only top-level declarations are considered (val/var at file scope).
     */
    private fun buildModuleBodyMap(file: KtFile): Map<String, KtBlockExpression> {
        val map = mutableMapOf<String, KtBlockExpression>()
        file.declarations.forEach { decl ->
            when (decl) {
                is KtProperty -> {
                    val name = decl.name ?: return@forEach
                    val init = decl.initializer as? KtCallExpression ?: return@forEach
                    if (init.calleeExpression?.text != "module") return@forEach
                    val body = (init.lambdaArguments.firstOrNull()?.getLambdaExpression()
                        ?: init.valueArguments.firstOrNull()?.getArgumentExpression() as? KtLambdaExpression)
                        ?.bodyExpression ?: return@forEach
                    map[name] = body
                }
                is KtNamedFunction -> {
                    val name = decl.name ?: return@forEach
                    // fun myModule() = module { ... }
                    val moduleCall = decl.bodyExpression as? KtCallExpression
                        ?: (decl.bodyBlockExpression
                            ?.statements?.lastOrNull() as? KtCallExpression)
                        ?: return@forEach
                    if (moduleCall.calleeExpression?.text != "module") return@forEach
                    val body = (moduleCall.lambdaArguments.firstOrNull()?.getLambdaExpression()
                        ?: moduleCall.valueArguments.firstOrNull()?.getArgumentExpression() as? KtLambdaExpression)
                        ?.bodyExpression ?: return@forEach
                    map[name] = body
                }
            }
        }
        return map
    }

    private fun checkModuleForDuplicates(
        moduleCall: KtCallExpression,
        moduleBodyByName: Map<String, KtBlockExpression>
    ) {
        val lambda = moduleCall.lambdaArguments.firstOrNull()?.getLambdaExpression()
            ?: moduleCall.valueArguments.firstOrNull()?.getArgumentExpression() as? KtLambdaExpression
            ?: return

        val bodyExpression = lambda.bodyExpression ?: return

        // Collect the names of modules referenced in includes() within this module's body
        val includedModuleNames = bodyExpression
            .collectDescendantsOfType<KtCallExpression>()
            .filter { it.calleeExpression?.text == "includes" }
            .flatMap { includesCall ->
                includesCall.valueArguments.mapNotNull { arg ->
                    // Each arg can be a bare reference (moduleA) or a call (moduleA())
                    val expr = arg.getArgumentExpression()
                    // bare reference: text is the name; call: calleeExpression?.text is the name
                    (expr as? KtCallExpression)?.calleeExpression?.text ?: expr?.text
                }
            }
            .toSet()

        if (includedModuleNames.isEmpty()) return

        // Collect all types registered in the included modules (those in the same file only)
        val typesInIncludedModules = mutableSetOf<String>()
        includedModuleNames.forEach { name ->
            val includedBody = moduleBodyByName[name] ?: return@forEach
            typesInIncludedModules += collectRegisteredTypes(includedBody)
            typesInIncludedModules += collectBoundTypes(includedBody)
        }

        if (typesInIncludedModules.isEmpty()) return

        // Collect registrations in this includes-module and flag conflicts
        val typesInThisModule = mutableListOf<TypeRegistration>()

        bodyExpression.collectDescendantsOfType<KtCallExpression>().forEach { call ->
            val callName = call.calleeExpression?.text
            if (callName in setOf("single", "factory", "scoped")) {
                val hasOverride = call.valueArguments.any { arg ->
                    val argName = arg.getArgumentName()?.asName?.asString()
                    argName == "override" && arg.getArgumentExpression()?.text == "true"
                }

                val typeArgs = call.typeArgumentList?.arguments
                val registeredType = typeArgs?.firstOrNull()?.text

                val hasQualifier = call.valueArguments.any { arg ->
                    val argText = arg.getArgumentExpression()?.text ?: ""
                    argText.contains("named(") || argText.contains("qualifier(")
                }
                if (registeredType != null && !hasOverride && !hasQualifier) {
                    typesInThisModule.add(TypeRegistration(registeredType, call, hasOverride))
                }

                // Check for bind syntax
                val parent = call.parent
                if (parent is KtBinaryExpression && parent.operationReference.text == "bind") {
                    val boundType = parent.right?.text?.replace("::class", "")?.trim()
                    if (boundType != null && !hasOverride && !hasQualifier) {
                        typesInThisModule.add(TypeRegistration(boundType, call, hasOverride))
                    }
                }
            }
        }

        // Report any type in this module that is also defined in an included module
        typesInThisModule.forEach { typeReg ->
            if (typeReg.type in typesInIncludedModules) {
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

    private fun collectRegisteredTypes(body: KtBlockExpression): Set<String> =
        body.collectDescendantsOfType<KtCallExpression>().mapNotNull { call ->
            val callName = call.calleeExpression?.text
            if (callName in setOf("single", "factory", "scoped")) {
                call.typeArgumentList?.arguments?.firstOrNull()?.text
            } else null
        }.toSet()

    private fun collectBoundTypes(body: KtBlockExpression): Set<String> =
        body.collectDescendantsOfType<KtBinaryExpression>()
            .filter { it.operationReference.text == "bind" }
            .mapNotNull { it.right?.text?.replace("::class", "")?.trim() }
            .toSet()
}
