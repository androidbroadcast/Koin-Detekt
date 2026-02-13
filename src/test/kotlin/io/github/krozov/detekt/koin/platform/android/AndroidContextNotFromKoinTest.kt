package io.github.krozov.detekt.koin.platform.android

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AndroidContextNotFromKoinTest {

    @Test
    fun `reports androidContext in module definition`() {
        val code = """
            val myModule = module {
                single { androidContext() }
            }
        """.trimIndent()

        val findings = AndroidContextNotFromKoin(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("androidContext")
    }

    @Test
    fun `allows androidContext in startKoin block`() {
        val code = """
            class MyApp : Application() {
                override fun onCreate() {
                    startKoin {
                        androidContext(this@MyApp)
                        modules(appModule)
                    }
                }
            }
        """.trimIndent()

        val findings = AndroidContextNotFromKoin(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports androidApplication outside startKoin`() {
        val code = """
            val context = androidApplication()
        """.trimIndent()

        val findings = AndroidContextNotFromKoin(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
