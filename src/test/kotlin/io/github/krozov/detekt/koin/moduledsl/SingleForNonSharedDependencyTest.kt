package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SingleForNonSharedDependencyTest {

    @Test
    fun `reports single for UseCase`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { GetUserUseCase(get()) }
            }
        """.trimIndent()

        val findings = SingleForNonSharedDependency(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("UseCase")
    }

    @Test
    fun `reports singleOf for Mapper`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.singleOf

            val myModule = module {
                singleOf(::UserMapper)
            }
        """.trimIndent()

        val findings = SingleForNonSharedDependency(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("Mapper")
    }

    @Test
    fun `does not report factory for UseCase`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                factory { GetUserUseCase(get()) }
            }
        """.trimIndent()

        val findings = SingleForNonSharedDependency(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `respects custom name patterns`() {
        val config = TestConfig(
            "namePatterns" to listOf(".*Command", ".*Handler")
        )

        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { ProcessOrderCommand() }
            }
        """.trimIndent()

        val findings = SingleForNonSharedDependency(config)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports single for Interactor`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                single { DataInteractor() }
            }
        """.trimIndent()

        val config = io.gitlab.arturbosch.detekt.test.TestConfig(
            "namePatterns" to listOf(".*Interactor", ".*Worker")
        )

        val findings = SingleForNonSharedDependency(config).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports single for Worker`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                single { BackgroundWorker() }
            }
        """.trimIndent()

        val config = io.gitlab.arturbosch.detekt.test.TestConfig(
            "namePatterns" to listOf(".*Interactor", ".*Worker")
        )

        val findings = SingleForNonSharedDependency(config).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports single for Handler`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                single { EventHandler() }
            }
        """.trimIndent()

        val config = io.gitlab.arturbosch.detekt.test.TestConfig(
            "namePatterns" to listOf(".*Handler")
        )

        val findings = SingleForNonSharedDependency(config).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports with enhanced message format`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                single { GetUserUseCase() }
            }
        """.trimIndent()

        val findings = SingleForNonSharedDependency(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }
}
