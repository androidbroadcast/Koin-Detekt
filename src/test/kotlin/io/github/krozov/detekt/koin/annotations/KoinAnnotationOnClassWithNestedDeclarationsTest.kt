package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KoinAnnotationOnClassWithNestedDeclarationsTest {

    @Test
    fun `reports Single class with nested class`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService {
                class Config(val url: String)
            }
        """.trimIndent()

        val findings = KoinAnnotationOnClassWithNestedDeclarations(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("Config")
    }

    @Test
    fun `reports Single class with nested object`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService {
                object Helper
            }
        """.trimIndent()

        val findings = KoinAnnotationOnClassWithNestedDeclarations(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report Single class with companion object`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService {
                companion object {
                    fun create() = MyService()
                }
            }
        """.trimIndent()

        val findings = KoinAnnotationOnClassWithNestedDeclarations(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report non-annotated class with nested class`() {
        val code = """
            class MyService {
                class Config(val url: String)
            }
        """.trimIndent()

        val findings = KoinAnnotationOnClassWithNestedDeclarations(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports Factory class with nested class`() {
        val code = """
            import org.koin.core.annotation.Factory

            @Factory
            class MyRepository {
                data class CacheEntry(val key: String, val value: Any)
            }
        """.trimIndent()

        val findings = KoinAnnotationOnClassWithNestedDeclarations(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `does not report Scoped class with only companion object`() {
        val code = """
            import org.koin.core.annotation.Scoped

            @Scoped
            class MyViewModel {
                companion object {
                    const val TAG = "MyViewModel"
                }
            }
        """.trimIndent()

        val findings = KoinAnnotationOnClassWithNestedDeclarations(Config.empty).lint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports class with both nested class and companion object`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService {
                class Config(val timeout: Int)

                companion object {
                    fun default() = MyService()
                }
            }
        """.trimIndent()

        val findings = KoinAnnotationOnClassWithNestedDeclarations(Config.empty).lint(code)

        // Only reports for the nested class, not the companion object
        assertThat(findings).hasSize(1)
    }
}
