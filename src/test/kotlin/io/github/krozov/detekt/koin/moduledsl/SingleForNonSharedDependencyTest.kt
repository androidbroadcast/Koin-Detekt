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

    // Edge case: singleOf with qualified type parameters
    @Test
    fun `reports singleOf with fully qualified UseCase type`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.singleOf

            val m = module {
                singleOf(::com.example.GetUserUseCase)
            }
        """.trimIndent()

        val findings = SingleForNonSharedDependency(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    // Edge case: Nested lambda with UseCase
    @Test
    fun `reports nested single with UseCase in lambda`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                single {
                    GetUserUseCase(
                        repository = get(),
                        validator = get()
                    )
                }
            }
        """.trimIndent()

        val findings = SingleForNonSharedDependency(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    // Edge case: Multiple UseCases with different patterns
    @Test
    fun `reports multiple non-shared dependencies with different patterns`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                single { GetUserUseCase() }
                single { UserMapper() }
                single { DataInteractor() }
            }
        """.trimIndent()

        val findings = SingleForNonSharedDependency(Config.empty).lint(code)
        assertThat(findings).hasSize(3)
    }

    // Edge case: UseCase with bind() modifier
    @Test
    fun `reports single with UseCase and bind modifier`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                single { GetUserUseCase() } bind UseCase::class
            }
        """.trimIndent()

        val findings = SingleForNonSharedDependency(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    // Edge case: singleOf in nested module
    @Test
    fun `reports singleOf for Mapper in nested module`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.singleOf

            val outerModule = module {
                includes(module {
                    singleOf(::DataMapper)
                })
            }
        """.trimIndent()

        val findings = SingleForNonSharedDependency(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    // Edge case: UseCase-like naming but not matching pattern
    @Test
    fun `does not report when type doesn't match any pattern`() {
        val code = """
            import org.koin.dsl.module

            val m = module {
                single { UseCaseFactory() }
                single { InteractorBuilder() }
            }
        """.trimIndent()

        val findings = SingleForNonSharedDependency(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
