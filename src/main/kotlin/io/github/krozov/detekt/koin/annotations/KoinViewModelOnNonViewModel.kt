package io.github.krozov.detekt.koin.annotations

import io.github.krozov.detekt.koin.util.ImportAwareRule
import io.github.krozov.detekt.koin.util.hasKoinAnnotationFrom
import io.github.krozov.detekt.koin.util.value
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass

/**
 * Detects `@KoinViewModel` on a class that does not extend `ViewModel` or `AndroidViewModel`.
 *
 * `@KoinViewModel` generates `viewModel { MyClass() }` DSL code. If the class does not extend
 * `ViewModel`, Koin will throw a `ClassCastException` at runtime when trying to cast it.
 * KSP does not validate the class hierarchy.
 *
 * **Note:** Uses heuristic supertype name matching. Classes ending with "ViewModel" are
 * treated as valid ViewModel base classes.
 *
 * <noncompliant>
 * @KoinViewModel
 * class MyPresenter  // ❌ ClassCastException at runtime
 * </noncompliant>
 *
 * <compliant>
 * @KoinViewModel
 * class MyViewModel : ViewModel()  // ✓ Correct base class
 * </compliant>
 */
internal class KoinViewModelOnNonViewModel(config: Config = Config.empty) : ImportAwareRule(config) {
    override val issue: Issue = Issue(
        id = "KoinViewModelOnNonViewModel",
        severity = Severity.Warning,
        description = "@KoinViewModel on class not extending ViewModel causes ClassCastException at runtime",
        debt = Debt.FIVE_MINS
    )

    private val additionalViewModelSuperTypes: List<String> =
        config.value(key = "additionalViewModelSuperTypes", default = emptyList())

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val hasKoinViewModelAnnotation = klass.hasKoinAnnotationFrom(importContext, setOf("KoinViewModel"))
        if (!hasKoinViewModelAnnotation) return

        val superTypeNames = klass.superTypeListEntries
            .mapNotNull { it.typeReference?.text }
            .map { it.substringBefore('(').substringBefore('<').substringAfterLast('.') }

        val extendsViewModel = superTypeNames.any { it.endsWith("ViewModel") || it in additionalViewModelSuperTypes }
        if (!extendsViewModel) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(klass),
                    """
                    @KoinViewModel on ${klass.name} which does not extend ViewModel
                    → @KoinViewModel generates viewModel {} DSL — class must extend ViewModel

                    ✗ Bad:  @KoinViewModel class ${klass.name}
                    ✓ Good: @KoinViewModel class ${klass.name} : ViewModel()
                    """.trimIndent()
                )
            )
        }
    }
}
