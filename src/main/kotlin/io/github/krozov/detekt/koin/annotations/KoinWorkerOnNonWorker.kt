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
 * Detects `@KoinWorker` on a class that does not extend `ListenableWorker` or `Worker`.
 *
 * `@KoinWorker` generates `worker { MyClass() }` DSL. If the class is not a valid Worker type,
 * WorkManager will fail to instantiate it at runtime. KSP does not validate the class hierarchy.
 *
 * **Note:** Uses heuristic supertype name matching — types ending with "Worker" are considered valid.
 *
 * <noncompliant>
 * @KoinWorker
 * class MyTask  // ❌ Not a Worker — runtime failure
 * </noncompliant>
 *
 * <compliant>
 * @KoinWorker
 * class MyWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params)
 * </compliant>
 */
internal class KoinWorkerOnNonWorker(config: Config = Config.empty) : ImportAwareRule(config) {
    override val issue: Issue = Issue(
        id = "KoinWorkerOnNonWorker",
        severity = Severity.Warning,
        description = "@KoinWorker on class not extending ListenableWorker causes runtime failure",
        debt = Debt.FIVE_MINS
    )

    private val additionalWorkerSuperTypes: List<String> =
        config.value(key = "additionalWorkerSuperTypes", default = emptyList())

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val hasKoinWorkerAnnotation = klass.hasKoinAnnotationFrom(importContext, setOf("KoinWorker"))
        if (!hasKoinWorkerAnnotation) return

        val superTypeNames = klass.superTypeListEntries
            .mapNotNull { it.typeReference?.text }
            .map { it.substringBefore('<').substringAfterLast('.') }

        val extendsWorker = superTypeNames.any { it.endsWith("Worker") || it in additionalWorkerSuperTypes }
        if (!extendsWorker) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(klass),
                    """
                    @KoinWorker on ${klass.name} which does not extend Worker or ListenableWorker
                    → @KoinWorker generates worker {} DSL — class must extend a Worker type

                    ✗ Bad:  @KoinWorker class ${klass.name}
                    ✓ Good: @KoinWorker class ${klass.name}(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params)
                    """.trimIndent()
                )
            )
        }
    }
}
