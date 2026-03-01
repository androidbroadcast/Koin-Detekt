package io.github.krozov.detekt.koin.annotations

import io.github.krozov.detekt.koin.util.ImportAwareRule
import io.github.krozov.detekt.koin.util.firstKoinAnnotationName
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass

/**
 * Detects `@Single`, `@Factory`, or `@Scoped` on abstract classes or interfaces.
 *
 * Abstract classes and interfaces cannot be instantiated. Koin Annotations will generate
 * `single { AbstractClass() }` which throws `InstantiationException` at runtime.
 * KSP does not validate instantiability.
 *
 * <noncompliant>
 * @Single
 * abstract class BaseRepository  // ❌ InstantiationException at runtime
 *
 * @Factory
 * interface Repository  // ❌ Cannot instantiate an interface
 * </noncompliant>
 *
 * <compliant>
 * @Single
 * class RepositoryImpl : Repository  // ✓ Concrete class
 * </compliant>
 */
internal class SingleOnAbstractClass(config: Config = Config.empty) : ImportAwareRule(config) {
    override val issue: Issue = Issue(
        id = "SingleOnAbstractClass",
        severity = Severity.Warning,
        description = "Koin annotation on abstract class or interface — cannot be instantiated at runtime",
        debt = Debt.FIVE_MINS
    )

    private val targetAnnotations = KoinAnnotationConstants.PROVIDER_ANNOTATIONS

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val koinAnnotation = klass.firstKoinAnnotationName(importContext, targetAnnotations) ?: return

        when {
            klass.isInterface() -> report(
                CodeSmell(
                    issue,
                    Entity.from(klass),
                    """
                    @$koinAnnotation on interface ${klass.name} — interfaces cannot be instantiated
                    → Remove the annotation or use on a concrete implementation

                    ✗ Bad:  @$koinAnnotation interface ${klass.name}
                    ✓ Good: @$koinAnnotation class ${klass.name}Impl : ${klass.name}
                    """.trimIndent()
                )
            )
            klass.hasModifier(KtTokens.ABSTRACT_KEYWORD) -> report(
                CodeSmell(
                    issue,
                    Entity.from(klass),
                    """
                    @$koinAnnotation on abstract class ${klass.name} — abstract classes cannot be instantiated
                    → Remove the annotation or use on a concrete subclass

                    ✗ Bad:  @$koinAnnotation abstract class ${klass.name}
                    ✓ Good: @$koinAnnotation class ${klass.name}Impl : ${klass.name}()
                    """.trimIndent()
                )
            )
        }
    }
}
