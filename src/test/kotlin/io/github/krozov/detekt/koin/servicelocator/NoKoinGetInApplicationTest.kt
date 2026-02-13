package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NoKoinGetInApplicationTest {

    @Test
    fun `reports get() inside startKoin block`() {
        val code = """
            import org.koin.core.context.startKoin

            fun main() {
                startKoin {
                    val service = get<MyService>()
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `does not report modules() inside startKoin`() {
        val code = """
            import org.koin.core.context.startKoin

            fun main() {
                startKoin {
                    modules(appModule)
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports inject() inside startKoin block`() {
        val code = """
            import org.koin.core.context.startKoin

            fun main() {
                startKoin {
                    val service: MyService by inject()
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports get() inside koinConfiguration block`() {
        val code = """
            import org.koin.core.context.koinConfiguration

            fun configure() {
                koinConfiguration {
                    val service = get<MyService>()
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports inject() inside koinConfiguration block`() {
        val code = """
            import org.koin.core.context.koinConfiguration

            fun configure() {
                koinConfiguration {
                    val service: MyService by inject()
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports multiple get() calls inside startKoin`() {
        val code = """
            import org.koin.core.context.startKoin

            fun main() {
                startKoin {
                    val service1 = get<Service1>()
                    val service2 = get<Service2>()
                    modules(appModule)
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(2)
    }

    @Test
    fun `reports nested get() inside startKoin with lambda`() {
        val code = """
            import org.koin.core.context.startKoin

            fun main() {
                startKoin {
                    modules(appModule)
                    listOf(1, 2).forEach {
                        val service = get<MyService>()
                    }
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report get() outside startKoin block`() {
        val code = """
            class MyClass(val koin: Koin) {
                fun doSomething() {
                    val service = koin.get<MyService>()
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report inject() outside startKoin block`() {
        val code = """
            class MyClass : KoinComponent {
                val service: MyService by inject()
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `handles nested startKoin blocks correctly`() {
        val code = """
            import org.koin.core.context.startKoin

            fun main() {
                startKoin {
                    modules(module {
                        single { get<OtherService>() }
                    })
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report get() in module definition outside startKoin`() {
        val code = """
            import org.koin.dsl.module

            val myModule = module {
                single { MyService(get()) }
            }

            fun main() {
                startKoin {
                    modules(myModule)
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports get() with qualifier inside startKoin`() {
        val code = """
            import org.koin.core.context.startKoin
            import org.koin.core.qualifier.named

            fun main() {
                startKoin {
                    val service = get<MyService>(named("special"))
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports inject() with parameters inside startKoin`() {
        val code = """
            import org.koin.core.context.startKoin

            fun main() {
                startKoin {
                    val service: MyService by inject { parametersOf("param") }
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `handles expressions without call name`() {
        val code = """
            import org.koin.core.context.startKoin

            fun main() {
                startKoin {
                    val value = "test"
                    println(value)
                }
            }
        """.trimIndent()

        val findings = NoKoinGetInApplication(Config.empty)
            .lint(code)

        assertThat(findings).isEmpty()
    }
}
