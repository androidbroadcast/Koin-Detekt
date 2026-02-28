package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MissingKoinStopInTestTest {

    @Test
    fun `reports test class with startKoin but no stopKoin in After`() {
        val code = """
            import org.junit.Before
            import org.junit.Test

            class MyTest {
                @Before
                fun setup() {
                    startKoin { modules(appModule) }
                }

                @Test
                fun `my test`() { }
            }
        """.trimIndent()

        val findings = MissingKoinStopInTest(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("stopKoin")
    }

    @Test
    fun `does not report when stopKoin is in After`() {
        val code = """
            import org.junit.After
            import org.junit.Before
            import org.junit.Test

            class MyTest {
                @Before
                fun setup() {
                    startKoin { modules(appModule) }
                }

                @After
                fun teardown() {
                    stopKoin()
                }

                @Test
                fun `my test`() { }
            }
        """.trimIndent()

        val findings = MissingKoinStopInTest(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report when stopKoin is in AfterEach`() {
        val code = """
            import org.junit.jupiter.api.AfterEach
            import org.junit.jupiter.api.BeforeEach
            import org.junit.jupiter.api.Test

            class MyTest {
                @BeforeEach
                fun setup() {
                    startKoin { modules(appModule) }
                }

                @AfterEach
                fun teardown() {
                    stopKoin()
                }

                @Test
                fun `my test`() { }
            }
        """.trimIndent()

        val findings = MissingKoinStopInTest(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report when stopKoin is in custom teardown annotation`() {
        val code = """
            class MyTest {
                @CustomAfter
                fun teardown() {
                    stopKoin()
                }

                fun setup() {
                    startKoin { modules(appModule) }
                }
            }
        """.trimIndent()

        val config = TestConfig("teardownAnnotations" to listOf("After", "AfterEach", "AfterAll", "CustomAfter"))
        val findings = MissingKoinStopInTest(config).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports when stopKoin is in unrecognized custom annotation`() {
        val code = """
            class MyTest {
                @CustomAfter
                fun teardown() {
                    stopKoin()
                }

                fun setup() {
                    startKoin { modules(appModule) }
                }
            }
        """.trimIndent()

        val findings = MissingKoinStopInTest(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report class without startKoin`() {
        val code = """
            import org.junit.Test

            class MyTest {
                @Test
                fun `my test`() { }
            }
        """.trimIndent()

        val findings = MissingKoinStopInTest(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports JUnit5 test class using startKoin without stopKoin`() {
        val code = """
            import org.junit.jupiter.api.BeforeEach
            import org.junit.jupiter.api.Test

            class MyTest {
                @BeforeEach
                fun setup() {
                    startKoin { modules(appModule) }
                }

                @Test
                fun `my test`() { }
            }
        """.trimIndent()

        val findings = MissingKoinStopInTest(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
