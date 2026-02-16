package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ModuleAsTopLevelValTest {

    @Test
    fun `reports module as top-level val`() {
        val code = """
            import org.koin.dsl.module

            val appModule = module { }
        """.trimIndent()

        val findings = ModuleAsTopLevelVal(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("top-level val")
        assertThat(findings[0].message).contains("Factory preallocation")
        assertThat(findings[0].message).contains("function")
    }

    @Test
    fun `reports module with definitions as top-level val`() {
        val code = """
            import org.koin.dsl.module

            val appModule = module {
                single { Service() }
                factory { Repository() }
            }
        """.trimIndent()

        val findings = ModuleAsTopLevelVal(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report module as function`() {
        val code = """
            import org.koin.dsl.module

            fun appModule() = module { }
        """.trimIndent()

        val findings = ModuleAsTopLevelVal(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report module as function with body`() {
        val code = """
            import org.koin.dsl.module

            fun appModule() = module {
                single { Service() }
                factory { Repository() }
            }
        """.trimIndent()

        val findings = ModuleAsTopLevelVal(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report module as var`() {
        val code = """
            import org.koin.dsl.module

            var appModule = module { }
        """.trimIndent()

        val findings = ModuleAsTopLevelVal(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report module inside class`() {
        val code = """
            import org.koin.dsl.module

            class ModuleProvider {
                val appModule = module { }
            }
        """.trimIndent()

        val findings = ModuleAsTopLevelVal(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report module inside object`() {
        val code = """
            import org.koin.dsl.module

            object Modules {
                val appModule = module { }
            }
        """.trimIndent()

        val findings = ModuleAsTopLevelVal(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report non-module val`() {
        val code = """
            val someValue = "test"
        """.trimIndent()

        val findings = ModuleAsTopLevelVal(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports multiple module vals`() {
        val code = """
            import org.koin.dsl.module

            val module1 = module { }
            val module2 = module { }
        """.trimIndent()

        val findings = ModuleAsTopLevelVal(Config.empty).lint(code)
        assertThat(findings).hasSize(2)
    }

    @Test
    fun `does not report module function with parameters`() {
        val code = """
            import org.koin.dsl.module

            fun appModule(debug: Boolean) = module {
                if (debug) {
                    single { DebugService() }
                }
            }
        """.trimIndent()

        val findings = ModuleAsTopLevelVal(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports module inside file with multiple declarations`() {
        val code = """
            import org.koin.dsl.module

            val appModule = module { single { Service() } }
            fun helperModule() = module { factory { Helper() } }
            val dataModule = module { single { Repository() } }
        """.trimIndent()

        val findings = ModuleAsTopLevelVal(Config.empty).lint(code)
        assertThat(findings).hasSize(2)
    }
}
