package io.github.krozov.detekt.koin.moduledsl

import io.github.krozov.detekt.koin.util.ImportAwareRule
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

/**
 * Detects constructor DSL calls (`factoryOf`, `singleOf`, `viewModelOf`, `scopedOf`) where the
 * referenced class has duplicate parameter types in its primary constructor, which causes Koin
 * to resolve all parameters of that type to the same instance.
 *
 * **Known limitation:** This rule only inspects the primary constructor when the referenced class
 * is defined in the **same file** as the module declaration. Cross-file class references are not
 * currently supported — the rule will silently produce no finding in the typical project structure
 * where classes and modules live in separate files.
 *
 * <noncompliant>
 * class MyService(val repo: Repository, val fallback: Repository)
 *
 * val appModule = module {
 *     factoryOf(::MyService) // Both Repository params resolve to the same instance
 * }
 * </noncompliant>
 *
 * <compliant>
 * class MyService(val repo: Repository, val fallback: Repository)
 *
 * val appModule = module {
 *     factory { MyService(get(named("primary")), get(named("fallback"))) }
 * }
 * </compliant>
 */
internal class ConstructorDslAmbiguousParameters(config: Config) : ImportAwareRule(config) {

    override val issue: Issue = Issue(
        id = "ConstructorDslAmbiguousParameters",
        severity = Severity.Warning,
        description = "Detects factoryOf/singleOf/viewModelOf/scopedOf with duplicate parameter types, " +
                "which causes Koin to resolve parameters incorrectly. " +
                "Note: detection is limited to classes defined in the same file as the module declaration; " +
                "cross-file references are not currently supported.",
        debt = Debt.TEN_MINS
    )

    private val constructorDslFunctions = setOf("factoryOf", "singleOf", "viewModelOf", "scopedOf")

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.calleeExpression?.text ?: return
        if (callName !in constructorDslFunctions) return
        // constructorDslFunctions are from org.koin.core.module.dsl — skip if imported elsewhere
        val fqns = importContext.resolveFqn(callName)
        if (fqns.isNotEmpty() && fqns.none { it.startsWith("org.koin.") }) return

        // Get constructor reference: factoryOf(::MyClass)
        val arg = expression.valueArguments.firstOrNull()?.text ?: return
        if (!arg.startsWith("::")) return

        val className = arg.removePrefix("::")

        // Heuristic: Look for class definition in same file
        val classDecl = findClassInFile(expression.containingKtFile, className) ?: return
        val constructor = classDecl.primaryConstructor ?: return

        val parameterTypes = constructor.valueParameters.map { param ->
            param.typeReference?.text?.removeSuffix("?") // Normalize Int? → Int
        }

        // Check for duplicates
        val duplicates = parameterTypes.groupingBy { it }.eachCount().filter { it.value > 1 }

        if (duplicates.isNotEmpty()) {
            val duplicateTypesList = duplicates.keys.joinToString(", ")
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    """
                    Constructor DSL with ambiguous parameters → Incorrect parameter resolution
                    → Found duplicate types: $duplicateTypesList
                    → Use lambda syntax for explicit resolution

                    ✗ Bad:  factoryOf(::$className)
                    ✓ Good: factory { $className(get(), get()) }
                    """.trimIndent()
                )
            )
        }
    }

    private fun findClassInFile(file: KtFile, className: String): KtClass? {
        return file.declarations.filterIsInstance<KtClass>()
            .firstOrNull { it.name == className }
    }
}
