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
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyRepo : KoinComponent {
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
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyRepo : KoinComponent {
                val service = get<ApiService>()
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports get in companion object`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyRepo : KoinComponent {
                companion object {
                    val service = get<ApiService>()
                }
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report qualified getAll call on non-Koin receiver`() {
        val code = """
            class AlarmRepositoryImpl(private val alarmDao: AlarmDao) {
                suspend fun getAlarms() = alarmDao.getAll().map { it.toAlarm() }
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report qualified get call on non-Koin receiver`() {
        val code = """
            class MyRepository(private val cache: Cache) {
                fun getUser(id: String) = cache.get(id)
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report get() in file without Koin imports`() {
        val code = """
            import java.util.concurrent.atomic.AtomicReference

            class MyClass {
                private val ref = AtomicReference<String>("value")
                fun getValue() = ref.get()
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report getOrNull() on Result in non-Koin file`() {
        val code = """
            fun process(result: Result<String>) = result.getOrNull()
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report getOrNull() on List in non-Koin file`() {
        val code = """
            fun first(list: List<String>) = list.getOrNull(0)
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report safe-qualified get call`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class MyRepository(private val cache: Cache?) : KoinComponent {
                fun getValue() = cache?.get("key")
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report safe-qualified getOrNull call`() {
        val code = """
            import org.koin.core.component.KoinComponent

            class MyRepository(private val result: Result<String>?) : KoinComponent {
                fun getValue() = result?.getOrNull()
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports unqualified get() in file with Koin imports despite non-Koin qualified calls`() {
        val code = """
            import org.koin.core.component.KoinComponent
            import org.koin.core.component.get

            class MyRepository(private val cache: Cache) : KoinComponent {
                val cachedValue = cache.get("key")  // qualified — safe
                val service = get<ApiService>()      // unqualified — Koin service locator
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report get() imported from non-Koin package`() {
        val code = """
            import com.example.cache.get

            class MyRepository {
                val value = get("key")
            }
        """.trimIndent()

        val findings = NoGetOutsideModuleDefinition(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
