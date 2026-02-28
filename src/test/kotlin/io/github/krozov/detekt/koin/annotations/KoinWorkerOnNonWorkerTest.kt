package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KoinWorkerOnNonWorkerTest {

    @Test
    fun `reports KoinWorker on class without Worker supertype`() {
        val code = """
            import org.koin.android.annotation.KoinWorker

            @KoinWorker
            class MyTask
        """.trimIndent()

        val findings = KoinWorkerOnNonWorker(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("Worker")
    }

    @Test
    fun `does not report KoinWorker on Worker subclass`() {
        val code = """
            import org.koin.android.annotation.KoinWorker

            @KoinWorker
            class MyWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : Worker(ctx, params)
        """.trimIndent()

        val findings = KoinWorkerOnNonWorker(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report KoinWorker on ListenableWorker subclass`() {
        val code = """
            import org.koin.android.annotation.KoinWorker

            @KoinWorker
            class MyWorker(ctx: android.content.Context, params: androidx.work.WorkerParameters) : ListenableWorker(ctx, params)
        """.trimIndent()

        val findings = KoinWorkerOnNonWorker(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report KoinWorker on CoroutineWorker subclass`() {
        val code = """
            import org.koin.android.annotation.KoinWorker

            @KoinWorker
            class MyWorker : CoroutineWorker(ctx, params)
        """.trimIndent()

        val findings = KoinWorkerOnNonWorker(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report class without KoinWorker annotation`() {
        val code = """
            class MyTask
        """.trimIndent()

        val findings = KoinWorkerOnNonWorker(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
