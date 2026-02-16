package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InjectedParamWithNestedGenericTypeTest {

    @Test
    fun `reports InjectedParam with nested generic type`() {
        val code = """
            import org.koin.core.annotation.InjectedParam
            import org.koin.core.annotation.Single

            @Single
            class MyService(@InjectedParam val items: List<List<String>>)
        """.trimIndent()

        val findings = InjectedParamWithNestedGenericType(Config.empty).lint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("nested generic")
    }

    @Test
    fun `reports InjectedParam with Map of Lists`() {
        val code = """
            import org.koin.core.annotation.InjectedParam
            import org.koin.core.annotation.Single

            @Single
            class MyService(@InjectedParam val map: Map<String, List<Int>>)
        """.trimIndent()

        val findings = InjectedParamWithNestedGenericType(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `reports InjectedParam with star projection`() {
        val code = """
            import org.koin.core.annotation.InjectedParam
            import org.koin.core.annotation.Single

            @Single
            class MyService(@InjectedParam val items: List<*>)
        """.trimIndent()

        val findings = InjectedParamWithNestedGenericType(Config.empty).lint(code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `allows InjectedParam with simple generic type`() {
        val code = """
            import org.koin.core.annotation.InjectedParam
            import org.koin.core.annotation.Single

            @Single
            class MyService(@InjectedParam val items: List<String>)
        """.trimIndent()

        val findings = InjectedParamWithNestedGenericType(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows InjectedParam with non-generic type`() {
        val code = """
            import org.koin.core.annotation.InjectedParam
            import org.koin.core.annotation.Single

            @Single
            class MyService(@InjectedParam val name: String)
        """.trimIndent()

        val findings = InjectedParamWithNestedGenericType(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `allows regular param with nested generic type`() {
        val code = """
            import org.koin.core.annotation.Single

            @Single
            class MyService(val items: List<List<String>>)
        """.trimIndent()

        val findings = InjectedParamWithNestedGenericType(Config.empty).lint(code)
        assertThat(findings).isEmpty()
    }
}
