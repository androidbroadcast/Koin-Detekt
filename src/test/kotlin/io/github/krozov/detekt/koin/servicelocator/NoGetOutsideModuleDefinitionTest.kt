package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NoGetOutsideModuleDefinitionTest {

    @Test
    fun `reports get() call outside module definition`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyRepository : KoinComponent {
                val api = get<ApiService>()
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("get()")
    }

    @Test
    fun `does not report get() inside single block`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { MyRepository(get()) }
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report get() inside factory block`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                factory { MyUseCase(get()) }
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }
}
