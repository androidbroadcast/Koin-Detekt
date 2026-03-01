package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PreferLazyConstructorInjectionTest {

    @Nested
    inner class PassiveMode {

        @Test
        fun `does not flag anything when both lazyTypes and checkAllTypes are not configured`() {
            val code = """
                class MyRepo(private val db: DatabaseClient)
            """.trimIndent()

            val findings = PreferLazyConstructorInjection(Config.empty).lint(code)

            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not flag when lazyTypes is empty list and checkAllTypes is false`() {
            val config = TestConfig(
                "checkAllTypes" to false,
                "lazyTypes" to emptyList<String>()
            )

            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(private val db: DatabaseClient)
            """.trimIndent())

            assertThat(findings).isEmpty()
        }
    }

    @Nested
    inner class LazyTypesAllowlist {

        private val config = TestConfig(
            "checkAllTypes" to false,
            "lazyTypes" to listOf("DatabaseClient")
        )

        @Test
        fun `flags parameter whose type is in lazyTypes`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(private val db: DatabaseClient)
            """.trimIndent())

            assertThat(findings).hasSize(1)
            assertThat(findings[0].message).contains("Lazy<DatabaseClient>")
        }

        @Test
        fun `does not flag parameter whose type is not in lazyTypes`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(private val api: HttpClient)
            """.trimIndent())

            assertThat(findings).isEmpty()
        }

        @Test
        fun `flags only matching parameters when constructor has mixed types`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(
                    private val db: DatabaseClient,
                    private val name: String,
                    private val api: HttpClient
                )
            """.trimIndent())

            assertThat(findings).hasSize(1)
            assertThat(findings[0].message).contains("Lazy<DatabaseClient>")
        }

        @Test
        fun `does not flag when type is already Lazy`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(private val db: Lazy<DatabaseClient>)
            """.trimIndent())

            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not flag nullable Lazy type`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(private val db: Lazy<DatabaseClient>?)
            """.trimIndent())

            assertThat(findings).isEmpty()
        }

        @Test
        fun `flags nullable type that matches lazyTypes`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(private val db: DatabaseClient?)
            """.trimIndent())

            assertThat(findings).hasSize(1)
            assertThat(findings[0].message).contains("Lazy<DatabaseClient?>")
        }

        @Test
        fun `flags multiple matching parameters`() {
            val multiTypeConfig = TestConfig(
                "checkAllTypes" to false,
                "lazyTypes" to listOf("DatabaseClient", "HttpClient")
            )

            val findings = PreferLazyConstructorInjection(multiTypeConfig).lint("""
                class MyRepo(
                    private val db: DatabaseClient,
                    private val client: HttpClient
                )
            """.trimIndent())

            assertThat(findings).hasSize(2)
        }
    }

    @Nested
    inner class ExcludeTypes {

        private val checkAllTypesWithStringExcluded = TestConfig(
            "checkAllTypes" to true,
            "excludeTypes" to listOf("String")
        )

        @Test
        fun `does not flag type that is in excludeTypes even if in lazyTypes`() {
            val config = TestConfig(
                "checkAllTypes" to false,
                "lazyTypes" to listOf("DatabaseClient"),
                "excludeTypes" to listOf("DatabaseClient")
            )

            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(private val db: DatabaseClient)
            """.trimIndent())

            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not flag excluded type in checkAllTypes mode`() {
            val findings = PreferLazyConstructorInjection(checkAllTypesWithStringExcluded).lint("""
                class MyRepo(private val name: String)
            """.trimIndent())

            assertThat(findings).isEmpty()
        }

        @Test
        fun `flags non-excluded type when checkAllTypes is true`() {
            val findings = PreferLazyConstructorInjection(checkAllTypesWithStringExcluded).lint("""
                class MyRepo(private val db: DatabaseClient)
            """.trimIndent())

            assertThat(findings).hasSize(1)
        }

        @Test
        fun `excludeTypes with multiple entries — none are flagged`() {
            val config = TestConfig(
                "checkAllTypes" to true,
                "excludeTypes" to listOf("String", "Int", "Boolean")
            )

            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(
                    private val name: String,
                    private val count: Int,
                    private val flag: Boolean
                )
            """.trimIndent())

            assertThat(findings).isEmpty()
        }
    }

    @Nested
    inner class CheckAllTypes {

        private val config = TestConfig("checkAllTypes" to true)

        @Test
        fun `flags any parameter type when checkAllTypes is true`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(private val db: DatabaseClient)
            """.trimIndent())

            assertThat(findings).hasSize(1)
        }

        @Test
        fun `does not flag Lazy type even with checkAllTypes true`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(private val db: Lazy<DatabaseClient>)
            """.trimIndent())

            assertThat(findings).isEmpty()
        }

        @Test
        fun `flags all parameters when checkAllTypes is true`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(
                    private val db: DatabaseClient,
                    private val api: HttpClient,
                    private val logger: Logger
                )
            """.trimIndent())

            assertThat(findings).hasSize(3)
        }

        @Test
        fun `flags types not in lazyTypes when checkAllTypes overrides it`() {
            val configWithBoth = TestConfig(
                "checkAllTypes" to true,
                "lazyTypes" to listOf("DatabaseClient")
            )

            val findings = PreferLazyConstructorInjection(configWithBoth).lint("""
                class MyRepo(private val api: HttpClient)
            """.trimIndent())

            assertThat(findings).hasSize(1)
        }
    }

    @Nested
    inner class ConstructorVariants {

        private val config = TestConfig(
            "checkAllTypes" to false,
            "lazyTypes" to listOf("DatabaseClient")
        )

        @Test
        fun `does not flag secondary constructor parameter`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo {
                    constructor(db: DatabaseClient)
                }
            """.trimIndent())

            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not flag class with no constructor parameters`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo()
            """.trimIndent())

            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not flag vararg parameter`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(vararg val dbs: DatabaseClient)
            """.trimIndent())

            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not flag function type parameter`() {
            val functionTypeConfig = TestConfig("checkAllTypes" to true)

            val findings = PreferLazyConstructorInjection(functionTypeConfig).lint("""
                class MyRepo(private val factory: () -> DatabaseClient)
            """.trimIndent())

            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not flag List of matching type`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(private val dbs: List<DatabaseClient>)
            """.trimIndent())

            assertThat(findings).isEmpty()
        }

        @Test
        fun `flags primary constructor when secondary constructor also exists`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(private val db: DatabaseClient) {
                    constructor() : this(DatabaseClient())
                }
            """.trimIndent())

            assertThat(findings).hasSize(1)
        }

        @Test
        fun `flags parameter with default value`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(private val db: DatabaseClient = DatabaseClient())
            """.trimIndent())

            assertThat(findings).hasSize(1)
        }

        @Test
        fun `does not flag List parameter when List is in excludeTypes`() {
            val listExcludeConfig = TestConfig(
                "checkAllTypes" to true,
                "excludeTypes" to listOf("List")
            )

            val findings = PreferLazyConstructorInjection(listExcludeConfig).lint("""
                class MyRepo(private val dbs: List<DatabaseClient>)
            """.trimIndent())

            assertThat(findings).isEmpty()
        }
    }

    @Nested
    inner class ClassKindVariants {

        private val config = TestConfig(
            "checkAllTypes" to false,
            "lazyTypes" to listOf("DatabaseClient")
        )

        @Test
        fun `flags parameter in data class`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                data class MyData(val db: DatabaseClient)
            """.trimIndent())

            assertThat(findings).hasSize(1)
        }

        @Test
        fun `flags parameter in abstract class`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                abstract class MyBase(val db: DatabaseClient)
            """.trimIndent())

            assertThat(findings).hasSize(1)
        }

        @Test
        fun `flags parameter in sealed class`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                sealed class MySealed(val db: DatabaseClient)
            """.trimIndent())

            assertThat(findings).hasSize(1)
        }

        @Test
        fun `does not flag object declaration`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                object MySingleton {
                    val db: DatabaseClient = DatabaseClient()
                }
            """.trimIndent())

            assertThat(findings).isEmpty()
        }

        @Test
        fun `does not flag interface`() {
            val findings = PreferLazyConstructorInjection(config).lint("""
                interface MyInterface {
                    val db: DatabaseClient
                }
            """.trimIndent())

            assertThat(findings).isEmpty()
        }
    }

    @Nested
    inner class FqnMatching {

        private val fqnConfig = TestConfig(
            "lazyTypes" to listOf("com.example.db.DatabaseClient")
        )

        @Test
        fun `flags when import resolves type to configured FQN`() {
            val findings = PreferLazyConstructorInjection(fqnConfig).lint("""
                import com.example.db.DatabaseClient

                class MyRepo(private val db: DatabaseClient)
            """.trimIndent())

            assertThat(findings).hasSize(1)
        }

        @Test
        fun `does not flag when import resolves to different package`() {
            val findings = PreferLazyConstructorInjection(fqnConfig).lint("""
                import com.other.DatabaseClient

                class MyRepo(private val db: DatabaseClient)
            """.trimIndent())

            assertThat(findings).isEmpty()
        }

        @Test
        fun `flags when type is written as FQN in constructor`() {
            val findings = PreferLazyConstructorInjection(fqnConfig).lint("""
                class MyRepo(private val db: com.example.db.DatabaseClient)
            """.trimIndent())

            assertThat(findings).hasSize(1)
        }

        @Test
        fun `falls back to short name match when star import is used`() {
            val findings = PreferLazyConstructorInjection(fqnConfig).lint("""
                import com.example.db.*

                class MyRepo(private val db: DatabaseClient)
            """.trimIndent())

            assertThat(findings).hasSize(1)
        }

        @Test
        fun `short-name entry in lazyTypes matches unqualified type without import`() {
            val config = TestConfig(
                "lazyTypes" to listOf("DatabaseClient")
            )

            val findings = PreferLazyConstructorInjection(config).lint("""
                class MyRepo(private val db: DatabaseClient)
            """.trimIndent())

            assertThat(findings).hasSize(1)
        }

        @Test
        fun `FQN entry in excludeTypes blocks match resolved via import`() {
            val config = TestConfig(
                "lazyTypes" to listOf("com.example.DatabaseClient"),
                "excludeTypes" to listOf("com.example.DatabaseClient")
            )

            val findings = PreferLazyConstructorInjection(config).lint("""
                import com.example.DatabaseClient

                class MyRepo(private val db: DatabaseClient)
            """.trimIndent())

            assertThat(findings).isEmpty()
        }

        @Test
        fun `flags generic type whose outer name resolves to configured FQN via import`() {
            val config = TestConfig(
                "lazyTypes" to listOf("com.example.db.DatabaseClient")
            )

            val findings = PreferLazyConstructorInjection(config).lint("""
                import com.example.db.DatabaseClient

                class MyRepo(private val dbs: DatabaseClient<String>)
            """.trimIndent())

            assertThat(findings).hasSize(1)
        }

        @Test
        fun `does not flag generic type when outer name resolves to different package`() {
            val config = TestConfig(
                "lazyTypes" to listOf("com.example.db.DatabaseClient")
            )

            val findings = PreferLazyConstructorInjection(config).lint("""
                import com.other.DatabaseClient

                class MyRepo(private val dbs: DatabaseClient<String>)
            """.trimIndent())

            assertThat(findings).isEmpty()
        }
    }
}
