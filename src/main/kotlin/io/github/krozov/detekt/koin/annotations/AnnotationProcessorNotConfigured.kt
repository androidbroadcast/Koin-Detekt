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
 * **This rule is inactive by default.** Enable it only if you want a
 * reminder to verify processor setup in new modules.
 *
 * The Koin annotation processor can be configured in two ways:
 *
 * **Modern approach (Koin 4.x recommended):**
 * ```
 * plugins {
 *     id("io.insert-koin.compiler.plugin") version "0.3.0"
 * }
 * ```
 *
 * **Legacy approach (manual KSP):**
 * ```
 * plugins {
 *     id("com.google.devtools.ksp") version "..."
 * }
 * dependencies {
 *     ksp("io.insert-koin:koin-ksp-compiler:...")
 * }
 * ```
 *
 * <noncompliant>
 * // build.gradle.kts missing both Koin Compiler Plugin and manual KSP setup
 *
 * @Single
 * class MyService // Won't work without processor!
 * </noncompliant>
 *
 * <compliant>
 * // Option 1: Koin Compiler Plugin (recommended for Koin 4.x)
 * // plugins { id("io.insert-koin.compiler.plugin") version "0.3.0" }
 *
 * // Option 2: Manual KSP setup
 * // plugins { id("com.google.devtools.ksp") version "..." }
 * // dependencies { ksp("io.insert-koin:koin-ksp-compiler:...") }
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
                    """
                    Koin annotations used → May not work without processor
                    → Ensure annotation processor is configured via one of:
                      1. Koin Compiler Plugin: plugins { id("io.insert-koin.compiler.plugin") }
                      2. Manual KSP: plugins { id("com.google.devtools.ksp") }; ksp("io.insert-koin:koin-ksp-compiler")

                    Set skipCheck=true or active=false in config if processor is already configured
                    """.trimIndent()
                )
            )
        }
    }
}
