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
 * Uses `@Single`/`@Factory` but KSP/KAPT may not be configured.
 *
 * Note: This rule provides informational warnings since Detekt cannot
 * reliably detect if annotation processor is configured in the build.
 *
 * <noncompliant>
 * // build.gradle.kts missing:
 * // plugins { id("com.google.devtools.ksp") }
 * // dependencies { ksp("io.insert-koin:koin-ksp-compiler") }
 *
 * @Single
 * class MyService // Won't work without processor!
 * </noncompliant>
 *
 * <compliant>
 * // build.gradle.kts with:
 * plugins {
 *     id("com.google.devtools.ksp") version "..."
 * }
 * dependencies {
 *     ksp("io.insert-koin:koin-ksp-compiler:...")
 * }
 *
 * @Single
 * class MyService
 * </compliant>
 */
public class AnnotationProcessorNotConfigured(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "AnnotationProcessorNotConfigured",
        severity = Severity.Warning,
        description = "Koin annotations used but processor may not be configured",
        debt = Debt.TEN_MINS
    )

    private val skipCheck = valueOrDefault("skipCheck", false)

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        if (skipCheck) return

        val annotations = klass.annotationEntries.mapNotNull { it.shortName?.asString() }
        if (annotations.any { it in setOf("Single", "Factory", "Scoped", "Module") }) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(klass),
                    "Class uses Koin annotations (@Single, @Factory, etc.). " +
                            "Ensure KSP or KAPT annotation processor is configured with koin-ksp-compiler. " +
                            "Otherwise, these definitions won't be generated. " +
                            "Configure skipCheck=true to disable this warning if processor is set up."
                )
            )
        }
    }
}
