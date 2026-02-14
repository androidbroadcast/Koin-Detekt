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
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
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

    @Test
    fun `reports getOrNull() call outside module definition`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.getOrNull

            class MyRepository : KoinComponent {
                val api = getOrNull<ApiService>()
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports getAll() call outside module definition`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.getAll

            class MyRepository : KoinComponent {
                val services = getAll<Service>()
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty)
            .lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("→")
        assertThat(findings[0].message).contains("✗ Bad")
        assertThat(findings[0].message).contains("✓ Good")
    }

    @Test
    fun `reports get in init block`() {
        val code = """
            import org.koin.core.component.get

            class MyRepo {
                init {
                    val service = get<ApiService>()
                }
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports get in property initializer`() {
        val code = """
            import org.koin.core.component.get

            class MyRepo {
                val service = get<ApiService>()
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports get in companion object`() {
        val code = """
            import org.koin.core.component.get

            class MyRepo {
                companion object {
                    val service = get<ApiService>()
                }
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report AtomicReference get()`() {
        val code = """
            import java.util.concurrent.atomic.AtomicReference

            class MyClass {
                val ref = AtomicReference("value")
                val current = ref.get()
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report SendChannel getOrNull()`() {
        val code = """
            import kotlinx.coroutines.channels.Channel

            suspend fun test() {
                val channel = Channel<String>()
                val value = channel.getOrNull()
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report Map get()`() {
        val code = """
            class MyClass {
                val map = mapOf("key" to "value")
                val value = map.get("key")
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report List getOrNull()`() {
        val code = """
            class MyClass {
                val list = listOf(1, 2, 3)
                val value = list.getOrNull(0)
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `still reports Koin get() with import`() {
        val code = """
            import org.koin.core.component.get

            class MyRepo {
                val service = get<ApiService>()
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }
}
