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
import org.jetbrains.kotlin.psi.KtTypeArgumentList

/**
 * Detects `get<ConcreteType>()` when only an interface is registered in the module.
 *
 * Koin's `Module.verify()` considers secondary types (bound interfaces), but runtime resolution doesn't.
 * This causes verify() to pass while runtime fails with `NoBeanDefFoundException`.
 *
 * This rule uses a heuristic approach: it warns when requesting a concrete implementation class
 * (especially those ending with "Impl") when definitions use `bind` or generic type parameters
 * that suggest interface registration.
 *
 * **Note:** This is a heuristic-based detection. Complex cases involving cross-module dependencies
 * may require semantic analysis beyond PSI capabilities.
 *
 * Reference: https://github.com/InsertKoinIO/koin/issues/2222
 *
 * <noncompliant>
 * interface Service
 * class ServiceImpl : Service
 *
 * val module = module {
 *     single<Service> { ServiceImpl() } // Only Service is registered
 *     factory { Consumer(get<ServiceImpl>()) } // ❌ Runtime fails
 * }
 * </noncompliant>
 *
 * <compliant>
 * interface Service
 * class ServiceImpl : Service
 *
 * val module = module {
 *     single<Service> { ServiceImpl() }
 *     factory { Consumer(get<Service>()) } // ✓ Request registered type
 * }
 *
 * // OR register both if needed
 * val module = module {
 *     single<ServiceImpl> { ServiceImpl() }
 *     single<Service> { get<ServiceImpl>() }
 *     factory { Consumer(get<ServiceImpl>()) } // ✓ Now OK
 * }
 * </compliant>
 */
public class GetConcreteTypeInsteadOfInterface(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "GetConcreteTypeInsteadOfInterface",
        severity = Severity.Warning,
        description = "get<ConcreteType>() fails at runtime when only interface is registered (verify() false positive)",
        debt = Debt.TEN_MINS
    )

    private val registeredTypes = mutableSetOf<String>()
    private val boundTypes = mutableMapOf<String, String>() // impl -> interface
    private val getRequests = mutableListOf<Pair<String, KtCallExpression>>()

    override fun visitKtFile(file: KtFile) {
        registeredTypes.clear()
        boundTypes.clear()
        getRequests.clear()
        super.visitKtFile(file)

        // Check for violations after analyzing entire file
        getRequests.forEach { (requestedType, expression) ->
            // If the requested type is bound to an interface and not directly registered
            if (boundTypes.containsKey(requestedType) && !registeredTypes.contains(requestedType)) {
                val boundInterface = boundTypes[requestedType]
                report(
                    CodeSmell(
                        issue,
                        Entity.from(expression),
                        """
                        get<$requestedType>() requests concrete type but only $boundInterface is registered
                        → verify() passes but runtime fails with NoBeanDefFoundException
                        → Request the interface type: get<$boundInterface>()

                        ✗ Bad:  single<$boundInterface> { $requestedType() }; get<$requestedType>()
                        ✓ Good: single<$boundInterface> { $requestedType() }; get<$boundInterface>()
                        """.trimIndent()
                    )
                )
            }
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.calleeExpression?.text ?: return

        // Track single/factory/scoped definitions
        if (callName in setOf("single", "factory", "scoped")) {
            // Check if it has a type parameter (e.g., single<Service>)
            val typeArgs = expression.typeArgumentList?.arguments
            val registeredType = typeArgs?.firstOrNull()?.text

            if (registeredType != null) {
                registeredTypes.add(registeredType)

                // Check for bind syntax: single<Interface> { ConcreteImpl() } bind OtherInterface::class
                // Or check the lambda body for class instantiation
                val lambdaBody = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()?.bodyExpression?.text
                if (lambdaBody != null) {
                    // Extract class name from instantiation (e.g., "ServiceImpl()" -> "ServiceImpl")
                    val implClass = Regex("""(\w+Impl|\w+Implementation)\s*\(""").find(lambdaBody)?.groupValues?.get(1)
                    if (implClass != null && implClass != registeredType) {
                        boundTypes[implClass] = registeredType
                    }
                }
            }

            // Also check for bind syntax
            val bindKeyword = expression.parent?.text?.contains("bind")
            if (bindKeyword == true) {
                val parentText = expression.parent?.text ?: ""
                // Extract implementation class from lambda
                val implClass = Regex("""(\w+Impl|\w+Implementation)\s*\(""").find(parentText)?.groupValues?.get(1)
                val boundInterface = Regex("""bind\s+(\w+)::class""").find(parentText)?.groupValues?.get(1)
                if (implClass != null && boundInterface != null) {
                    boundTypes[implClass] = boundInterface
                }
            }
        }

        // Track get<Type>() calls
        if (callName == "get") {
            val typeArgs = expression.typeArgumentList?.arguments
            val requestedType = typeArgs?.firstOrNull()?.text
            if (requestedType != null) {
                getRequests.add(requestedType to expression)
            }
        }
    }
}
