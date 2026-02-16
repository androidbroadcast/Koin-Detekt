package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Detects Koin definition annotations on extension functions.
 *
 * KSP code generator ignores the receiver parameter on extension functions,
 * producing invalid generated code.
 *
 * <noncompliant>
 * @Module
 * class MyModule {
 *     @Single
 *     fun Scope.provideDatastore(): DataStore = DataStore()
 * }
 * </noncompliant>
 *
 * <compliant>
 * @Module
 * class MyModule {
 *     @Single
 *     fun provideDatastore(scope: Scope): DataStore = DataStore()
 * }
 * </compliant>
 */
public class KoinAnnotationOnExtensionFunction(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "KoinAnnotationOnExtensionFunction",
        severity = Severity.Warning,
        description = "Koin annotation on extension function — KSP ignores receiver parameter",
        debt = Debt.FIVE_MINS
    )

    private val koinDefinitionAnnotations = setOf(
        "Single", "Factory", "Scoped", "KoinViewModel", "KoinWorker"
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        if (function.receiverTypeReference == null) return

        val annotations = function.annotationEntries.mapNotNull { it.shortName?.asString() }
        val koinAnnotation = annotations.firstOrNull { it in koinDefinitionAnnotations } ?: return

        val receiverType = function.receiverTypeReference?.text ?: "Unknown"

        report(
            CodeSmell(
                issue,
                Entity.from(function),
                """
                @$koinAnnotation on extension function — KSP ignores receiver '$receiverType'
                → Convert to regular function with explicit parameter

                ✗ Bad:  @$koinAnnotation fun $receiverType.${function.name}()
                ✓ Good: @$koinAnnotation fun ${function.name}(${receiverType.lowercase()}: $receiverType)
                """.trimIndent()
            )
        )
    }
}
