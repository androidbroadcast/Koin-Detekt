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
 * Informational rule that flags Koin annotation usage as a reminder to verify
 * annotation processor configuration in your build.
 *
 * **Important:** This rule is informational only. Detekt operates on Kotlin source
 * code and cannot read Gradle build files, so it cannot verify whether an annotation
 * processor is actually configured. Findings from this rule do not necessarily
 * indicate a real problem.
 *
 * Two valid setup methods exist:
 *
 * **KSP (legacy):**
 * ```kotlin
 * plugins { id("com.google.devtools.ksp") }
 * dependencies { ksp("io.insert-koin:koin-ksp-compiler") }
 * ```
 *
 * **Koin Compiler Plugin (modern, recommended):**
 * ```kotlin
 * plugins { id("io.insert-koin.compiler.plugin") }
 * ```
 *
 * If your project is already configured correctly, suppress this rule by adding
 * `skipCheck: true` to your detekt configuration:
 * ```yaml
 * AnnotationProcessorNotConfigured:
 *   skipCheck: true
 * ```
 *
 * <noncompliant>
 * // build.gradle.kts missing processor setup entirely:
 * // (no KSP plugin and no Koin Compiler Plugin)
 *
 * @Single
 * class MyService // annotations will be ignored at runtime\!
 * </noncompliant>
 *
 * <compliant>
 * // build.gradle.kts — option A (KSP):
 * plugins {
 *     id("com.google.devtools.ksp") version "..."
 * }
 * dependencies {
 *     ksp("io.insert-koin:koin-ksp-compiler:...")
 * }
 *
 * // build.gradle.kts — option B (Koin Compiler Plugin):
 * plugins {
 *     id("io.insert-koin.compiler.plugin") version "..."
 * }
 *
 * @Single
 * class MyService
 * </compliant>
 */
public class AnnotationProcessorNotConfigured(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "AnnotationProcessorNotConfigured",
        severity = Severity.Minor,
        description = "Koin annotation found — annotation processor configuration cannot be verified by static analysis",
        debt = Debt.TEN_MINS
    )

    private val skipCheck = valueOrDefault("skipCheck", false)

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        if (skipCheck) return

        val annotations = klass.annotationEntries.mapNotNull { it.shortName?.asString() }
        if (annotations.any { it in KoinAnnotationConstants.ALL_ANNOTATIONS }) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(klass),
                    """
                    Koin annotation found — annotation processor configuration cannot be verified by static analysis.

                    If using KSP:
                      ✓ plugins { id("com.google.devtools.ksp") }
                      ✓ ksp("io.insert-koin:koin-ksp-compiler")

                    If using Koin Compiler Plugin (modern, recommended):
                      ✓ plugins { id("io.insert-koin.compiler.plugin") }

                    Set skipCheck=true in detekt config to suppress this warning if processor is configured.
                    """.trimIndent()
                )
            )
        }
    }
}
