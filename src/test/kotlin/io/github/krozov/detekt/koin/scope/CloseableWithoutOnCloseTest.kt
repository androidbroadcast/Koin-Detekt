package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CloseableWithoutOnCloseTest {

    @Test
    fun `reports Closeable defined as single without onClose`() {
        val code = """
            import org.koin.dsl.module
            import java.io.Closeable

            class DatabaseConnection : Closeable {
                override fun close() { }
            }

            val appModule = module {
                single { DatabaseConnection() }
            }
        """.trimIndent()

        val findings = CloseableWithoutOnClose(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("Closeable")
        assertThat(findings[0].message).contains("onClose")
    }

    @Test
    fun `reports AutoCloseable without onClose`() {
        val code = """
            import org.koin.dsl.module

            class MyResource : AutoCloseable {
                override fun close() { }
            }

            val appModule = module {
                single { MyResource() }
            }
        """.trimIndent()

        val findings = CloseableWithoutOnClose(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports scoped Closeable without onClose`() {
        val code = """
            import org.koin.dsl.module
            import java.io.Closeable

            class Connection : Closeable {
                override fun close() { }
            }

            val appModule = module {
                scoped { Connection() }
            }
        """.trimIndent()

        val findings = CloseableWithoutOnClose(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report Closeable with onClose`() {
        val code = """
            import org.koin.dsl.module
            import java.io.Closeable

            class Connection : Closeable {
                override fun close() { }
            }

            val appModule = module {
                single { Connection() } onClose { it?.close() }
            }
        """.trimIndent()

        val findings = CloseableWithoutOnClose(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report non-Closeable types`() {
        val code = """
            import org.koin.dsl.module

            class MyRepository

            val appModule = module {
                single { MyRepository() }
            }
        """.trimIndent()

        val findings = CloseableWithoutOnClose(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
