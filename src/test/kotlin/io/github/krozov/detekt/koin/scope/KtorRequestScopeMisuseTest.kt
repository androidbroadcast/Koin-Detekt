package io.github.krozov.detekt.koin.scope

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KtorRequestScopeMisuseTest {

    @Test
    fun `reports single inside requestScope`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                requestScope {
                    single { RequestLogger() }
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports singleOf inside requestScope`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.singleOf

            val myModule = module {
                requestScope {
                    singleOf(::RequestHandler)
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report scoped inside requestScope`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                requestScope {
                    scoped { RequestLogger() }
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report single outside requestScope`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { Logger() }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report factory inside requestScope`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                requestScope {
                    factory { RequestLogger() }
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report viewModel inside requestScope`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                requestScope {
                    viewModel { RequestViewModel() }
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports multiple single calls inside requestScope`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                requestScope {
                    single { Logger() }
                    single { Metrics() }
                    scoped { Handler() }
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(2)
    }

    @Test
    fun `reports single with qualifier inside requestScope`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.qualifier.named

            val myModule = module {
                requestScope {
                    single(named("logger")) { RequestLogger() }
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports singleOf with parameters inside requestScope`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.singleOf

            val myModule = module {
                requestScope {
                    singleOf(::RequestHandler)
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `handles nested requestScope correctly`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                requestScope {
                    scoped { OuterHandler() }
                    requestScope {
                        single { InnerLogger() }
                    }
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report singleOf outside requestScope`() {
        val code = """
            import org.koin.dsl.module
            import org.koin.core.module.dsl.singleOf

            val myModule = module {
                singleOf(::GlobalService)
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports single in lambda inside requestScope`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                requestScope {
                    listOf("a", "b").forEach {
                        single { Logger() }
                    }
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `handles empty requestScope`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                requestScope { }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report worker inside requestScope`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                requestScope {
                    worker { RequestWorker() }
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles mixed valid and invalid definitions`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                requestScope {
                    scoped { ValidHandler() }
                    factory { ValidFactory() }
                    single { InvalidSingleton() }
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `handles expressions without call name in requestScope`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                requestScope {
                    val config = loadConfig()
                    scoped { Handler(config) }
                }
            }
        """.trimIndent()

        val findings = KtorRequestScopeMisuse(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }
}
