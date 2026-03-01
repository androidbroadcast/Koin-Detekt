package io.github.krozov.detekt.koin.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KoinSymbolsTest {

    @Test
    fun `known koin annotation names are recognised`() {
        listOf("Single", "Factory", "Scoped", "Module", "Qualifier",
               "KoinViewModel", "KoinWorker", "ComponentScan",
               "Configuration", "KoinApplication").forEach { name ->
            assertThat(KoinSymbols.isKnownName(name))
                .`as`("$name should be known").isTrue()
        }
    }

    @Test
    fun `known koin DSL names are recognised`() {
        listOf("single", "factory", "scoped", "viewModel", "worker",
               "module", "get", "getOrNull", "getAll", "inject").forEach { name ->
            assertThat(KoinSymbols.isKnownName(name))
                .`as`("$name should be known").isTrue()
        }
    }

    @Test
    fun `unknown names return false`() {
        listOf("Foo", "Bar", "Inject", "Component", "Bean").forEach { name ->
            assertThat(KoinSymbols.isKnownName(name))
                .`as`("$name should not be known").isFalse()
        }
    }

    @Test
    fun `koin packages cover core modules`() {
        listOf(
            "org.koin.core.annotation",
            "org.koin.dsl",
            "org.koin.core.component",
            "org.koin.android.ext.android",
            "org.koin.androidx.viewmodel.ext.android",
            "org.koin.ktor.ext",
        ).forEach { pkg ->
            assertThat(KoinSymbols.KOIN_PACKAGES)
                .`as`("$pkg should be in KOIN_PACKAGES").contains(pkg)
        }
    }
}
