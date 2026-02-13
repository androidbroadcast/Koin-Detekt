package io.github.krozov.detekt.koin.platform.compose

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RememberKoinModulesLeakTest {

    @Test
    fun `reports loadKoinModules in remember without unload`() {
        val code = """
            @Composable
            fun FeatureScreen() {
                remember { loadKoinModules(featureModule) }
            }
        """.trimIndent()

        val findings = RememberKoinModulesLeak(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("loadKoinModules")
        assertThat(findings[0].message).contains("remember")
    }

    @Test
    fun `allows loadKoinModules with DisposableEffect`() {
        val code = """
            @Composable
            fun FeatureScreen() {
                DisposableEffect(Unit) {
                    loadKoinModules(featureModule)
                    onDispose { unloadKoinModules(featureModule) }
                }
            }
        """.trimIndent()

        val findings = RememberKoinModulesLeak(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows remember without loadKoinModules`() {
        val code = """
            @Composable
            fun MyScreen() {
                val state = remember { mutableStateOf(0) }
            }
        """.trimIndent()

        val findings = RememberKoinModulesLeak(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
