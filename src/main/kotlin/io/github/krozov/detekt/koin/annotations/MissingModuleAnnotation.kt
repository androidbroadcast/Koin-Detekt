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
 * Class has `@Single`/`@Factory` but no `@Module` annotation.
 *
 * Annotation processor won't find definitions without `@Module`.
 *
 * <noncompliant>
 * class MyServices {
 *     @Single
 *     fun provideRepo(): Repository = ... // Won't be discovered!
 * }
 * </noncompliant>
 *
 * <compliant>
 * @Module
 * class MyServices {
 *     @Single
 *     fun provideRepo(): Repository = ...
 * }
 * </compliant>
 */
public class MissingModuleAnnotation(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "MissingModuleAnnotation",
        severity = Severity.Warning,
        description = "Class with @Single/@Factory needs @Module annotation",
        debt = Debt.FIVE_MINS
    )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val classAnnotations = klass.annotationEntries.mapNotNull { it.shortName?.asString() }
        val hasModuleAnnotation = "Module" in classAnnotations

        if (!hasModuleAnnotation) {
            // Check if class has methods with Koin annotations
            val hasKoinDefinitions = klass.declarations.any { declaration ->
                val annotations = declaration.annotationEntries.mapNotNull { it.shortName?.asString() }
                annotations.any { it in setOf("Single", "Factory", "Scoped") }
            }

            if (hasKoinDefinitions) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(klass),
                        "Class '${klass.name}' has @Single/@Factory/@Scoped but no @Module annotation. " +
                                "Annotation processor won't discover these definitions without @Module."
                    )
                )
            }
        }
    }
}
