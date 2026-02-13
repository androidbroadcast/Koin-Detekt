package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NoGlobalContextAccessTest {

    @Test
    fun `reports GlobalContext get() access`() {
        val code = """
            import org.koin.core.context.GlobalContext

            fun getService() {
                val koin = GlobalContext.get()
            }
        """.trimIndent()

        val findings = NoGlobalContextAccess(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports GlobalContext getKoinApplicationOrNull`() {
        val code = """
            import org.koin.core.context.GlobalContext

            val app = GlobalContext.getKoinApplicationOrNull()
        """.trimIndent()

        val findings = NoGlobalContextAccess(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report startKoin usage`() {
        val code = """
            import org.koin.core.context.startKoin

            fun main() {
                startKoin {
                    modules(appModule)
                }
            }
        """.trimIndent()

        val findings = NoGlobalContextAccess(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports KoinPlatformTools get() access`() {
        val code = """
            import org.koin.core.KoinPlatformTools

            fun getKoin() {
                val koin = KoinPlatformTools.defaultContext().get()
            }
        """.trimIndent()

        val findings = NoGlobalContextAccess(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports GlobalContext getOrNull access`() {
        val code = """
            import org.koin.core.context.GlobalContext

            fun getService() {
                val koin = GlobalContext.getOrNull()
            }
        """.trimIndent()

        val findings = NoGlobalContextAccess(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports GlobalContext stopKoin access`() {
        val code = """
            import org.koin.core.context.GlobalContext

            fun cleanup() {
                GlobalContext.stopKoin()
            }
        """.trimIndent()

        val findings = NoGlobalContextAccess(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports multiple GlobalContext accesses`() {
        val code = """
            import org.koin.core.context.GlobalContext

            fun example() {
                val koin1 = GlobalContext.get()
                val koin2 = GlobalContext.getKoinApplicationOrNull()
                GlobalContext.stopKoin()
            }
        """.trimIndent()

        val findings = NoGlobalContextAccess(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(3)
    }

    @Test
    fun `does not report non-receiver GlobalContext variable`() {
        val code = """
            fun example() {
                val GlobalContext = "not the real thing"
                println(GlobalContext)
            }
        """.trimIndent()

        val findings = NoGlobalContextAccess(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports GlobalContext in nested function`() {
        val code = """
            import org.koin.core.context.GlobalContext

            class MyClass {
                fun outer() {
                    fun inner() {
                        val koin = GlobalContext.get()
                    }
                }
            }
        """.trimIndent()

        val findings = NoGlobalContextAccess(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports GlobalContext in lambda`() {
        val code = """
            import org.koin.core.context.GlobalContext

            fun example() {
                listOf(1, 2, 3).map {
                    GlobalContext.get()
                }
            }
        """.trimIndent()

        val findings = NoGlobalContextAccess(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report similar named classes`() {
        val code = """
            object MyGlobalContext {
                fun get() = "data"
            }

            fun example() {
                val result = MyGlobalContext.get()
            }
        """.trimIndent()

        val findings = NoGlobalContextAccess(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }
}
