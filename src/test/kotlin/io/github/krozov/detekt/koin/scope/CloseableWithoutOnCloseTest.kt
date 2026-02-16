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
}
