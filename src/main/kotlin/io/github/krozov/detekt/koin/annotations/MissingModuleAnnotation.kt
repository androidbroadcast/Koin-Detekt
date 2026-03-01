package io.github.krozov.detekt.koin.annotations

import io.github.krozov.detekt.koin.util.ImportAwareRule
import io.github.krozov.detekt.koin.util.Resolution
import io.github.krozov.detekt.koin.util.hasKoinAnnotationFrom
import io.github.krozov.detekt.koin.util.resolveKoin
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
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
internal class MissingModuleAnnotation(config: Config = Config.empty) : ImportAwareRule(config) {
    override val issue: Issue = Issue(
        id = "MissingModuleAnnotation",
        severity = Severity.Warning,
        description = "Class with @Single/@Factory needs @Module annotation",
        debt = Debt.FIVE_MINS
    )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val hasModuleAnnotation = klass.hasKoinAnnotationFrom(importContext, setOf("Module"))

        if (!hasModuleAnnotation) {
            // Check if class has methods with Koin annotations
            val hasKoinDefinitions = klass.declarations.any { declaration ->
                declaration.hasKoinAnnotationFrom(importContext, KoinAnnotationConstants.DEFINITION_ANNOTATIONS)
            }

            if (hasKoinDefinitions) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(klass),
                        """
                        Koin definition annotations without @Module → Definitions won't be discovered
                        → Add @Module annotation to class for annotation processor

                        ✗ Bad:  class ${klass.name ?: "MyServices"} { @Single fun provideRepo() = ... }
                        ✓ Good: @Module class ${klass.name ?: "MyServices"} { @Single fun provideRepo() = ... }
                        """.trimIndent()
                    )
                )
            }
        }

        if (hasModuleAnnotation) {
            val hasComponentScan = klass.hasKoinAnnotationFrom(importContext, setOf("ComponentScan"))
            // @Configuration signals Koin Compiler Plugin auto-discovery; module is valid as-is
            val hasConfiguration = klass.hasKoinAnnotationFrom(importContext, setOf("Configuration"))
            val hasIncludes = klass.annotationEntries.any { entry ->
                entry.shortName?.asString() == "Module" &&
                    importContext.resolveKoin("Module") != Resolution.NOT_KOIN &&
                    entry.valueArgumentList?.arguments?.any { arg ->
                        arg.getArgumentName()?.asName?.asString() == "includes"
                    } == true
            }
            val hasKoinDefinitions = klass.declarations.any { declaration ->
                declaration.hasKoinAnnotationFrom(importContext, KoinAnnotationConstants.DEFINITION_ANNOTATIONS)
            }

            if (!hasComponentScan && !hasConfiguration && !hasIncludes && !hasKoinDefinitions) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(klass),
                        """
                        @Module without @ComponentScan, includes, or definitions → Module will be empty
                        → Add @ComponentScan or include other modules or add provider functions

                        ✗ Bad:  @Module class ${klass.name ?: "EmptyModule"}
                        ✓ Good: @Module @ComponentScan class ${klass.name ?: "EmptyModule"}
                        """.trimIndent()
                    )
                )
            }
        }
    }
}
