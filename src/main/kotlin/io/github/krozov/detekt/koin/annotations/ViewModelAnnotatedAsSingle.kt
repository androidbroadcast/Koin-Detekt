package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass

/**
 * Detects ViewModel classes annotated with `@Single` or `@Factory` instead of `@KoinViewModel`.
 *
 * ViewModels as singletons cause coroutine scope issues: `viewModelScope` is cancelled
 * on navigation but the singleton persists, leading to `CancellationException`.
 *
 * **Limitation:** This rule uses PSI-based analysis and cannot detect transitive ViewModel
 * inheritance (e.g., `BaseViewModel -> ViewModel`). It checks direct supertypes for
 * `ViewModel`/`AndroidViewModel` names and also applies a heuristic for supertype names
 * ending with "ViewModel".
 *
 * <noncompliant>
 * @Single
 * class MyViewModel : ViewModel() // Coroutine failures after navigation!
 * </noncompliant>
 *
 * <compliant>
 * @KoinViewModel
 * class MyViewModel : ViewModel()
 * </compliant>
 */
public class ViewModelAnnotatedAsSingle(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "ViewModelAnnotatedAsSingle",
        severity = Severity.Warning,
        description = "ViewModel should use @KoinViewModel, not @Single/@Factory",
        debt = Debt.FIVE_MINS
    )

    private val wrongAnnotations = setOf("Single", "Factory")
    private val viewModelTypes = setOf("ViewModel", "AndroidViewModel")

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val annotations = klass.annotationEntries.mapNotNull { it.shortName?.asString() }
        val wrongAnnotation = annotations.firstOrNull { it in wrongAnnotations } ?: return

        // Check supertypes for ViewModel
        val supertypes = klass.superTypeListEntries.mapNotNull { entry ->
            entry.typeReference?.text?.substringBefore("(")?.substringAfterLast(".")?.trim()
        }

        if (supertypes.any { it in viewModelTypes || it.endsWith("ViewModel") }) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(klass),
                    """
                    ViewModel annotated with @$wrongAnnotation instead of @KoinViewModel
                    → ViewModels need lifecycle-aware scoping, not singleton/factory

                    ✗ Bad:  @$wrongAnnotation class ${klass.name} : ViewModel()
                    ✓ Good: @KoinViewModel class ${klass.name} : ViewModel()
                    """.trimIndent()
                )
            )
        }
    }
}
