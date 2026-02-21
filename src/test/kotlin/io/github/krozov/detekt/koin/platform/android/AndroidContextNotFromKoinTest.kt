package io.github.krozov.detekt.koin.platform.android

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AndroidContextNotFromKoinTest {

    @Test
    fun `allows androidContext in module definition`() {
        val code = """
            val myModule = module {
                single { androidContext() }
            }
        """.trimIndent()

        val findings = AndroidContextNotFromKoin(Config.empty).lint(code)
        assertThat(findings).isEmpty()
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
    fun `reports androidApplication outside Koin context`() {
        val code = """
            val context = androidApplication()
        """.trimIndent()

        val findings = AndroidContextNotFromKoin(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports androidContext outside Koin context`() {
        val code = """
            fun setup() {
                val ctx = androidContext()
            }
        """.trimIndent()

        val findings = AndroidContextNotFromKoin(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("outside Koin context")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `allows androidContext inside single block in module`() {
        val code = """
            val myModule = module {
                single { MyRepository(androidContext()) }
            }
        """.trimIndent()

        val findings = AndroidContextNotFromKoin(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows androidContext inside factory block in module`() {
        val code = """
            val myModule = module {
                factory { MyService(androidContext()) }
            }
        """.trimIndent()

        val findings = AndroidContextNotFromKoin(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows androidContext inside scoped block in module`() {
        val code = """
            val myModule = module {
                scoped { MyManager(androidContext()) }
            }
        """.trimIndent()

        val findings = AndroidContextNotFromKoin(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows androidContext inside viewModel block in module`() {
        val code = """
            val myModule = module {
                viewModel { MyViewModel(androidApplication()) }
            }
        """.trimIndent()

        val findings = AndroidContextNotFromKoin(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows androidContext inside worker block in module`() {
        val code = """
            val myModule = module {
                worker { MyWorker(androidContext()) }
            }
        """.trimIndent()

        val findings = AndroidContextNotFromKoin(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports androidContext in plain class`() {
        val code = """
            class MyClass {
                val ctx = androidContext()
            }
        """.trimIndent()

        val findings = AndroidContextNotFromKoin(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows androidApplication inside module directly`() {
        val code = """
            val myModule = module {
                androidApplication()
            }
        """.trimIndent()

        val findings = AndroidContextNotFromKoin(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
    @Test
    fun `does not report androidContext inside initKoin`() {
        val code = """
            import org.koin.android.ext.koin.androidContext
            fun setup(app: Application) {
                initKoin {
                    androidContext(app)
                }
            }
        """.trimIndent()

        val findings = AndroidContextNotFromKoin(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

}
