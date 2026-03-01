package io.github.krozov.detekt.koin.util

import io.github.detekt.test.utils.compileContentForTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FileImportContextTest {

    @Nested
    inner class ExactImports {
        @Test
        fun `resolves koin exact import`() {
            val ctx = ctx("""
                package com.example
                import org.koin.core.annotation.Single
                class Foo
            """)
            assertThat(ctx.resolveFqn("Single"))
                .containsExactly("org.koin.core.annotation.Single")
        }

        @Test
        fun `resolves non-koin exact import`() {
            val ctx = ctx("""
                package com.example
                import javax.inject.Single
                class Foo
            """)
            assertThat(ctx.resolveFqn("Single"))
                .containsExactly("javax.inject.Single")
        }

        @Test
        fun `returns empty when name not imported`() {
            val ctx = ctx("""
                package com.example
                import org.koin.core.annotation.Factory
                class Foo
            """)
            assertThat(ctx.resolveFqn("Single")).isEmpty()
        }
    }

    @Nested
    inner class AliasImports {
        @Test
        fun `resolves alias to original FQN`() {
            val ctx = ctx("""
                package com.example
                import org.koin.core.annotation.Single as KoinSingle
                class Foo
            """)
            assertThat(ctx.resolveFqn("KoinSingle"))
                .containsExactly("org.koin.core.annotation.Single")
        }

        @Test
        fun `original name no longer resolves when aliased`() {
            val ctx = ctx("""
                package com.example
                import org.koin.core.annotation.Single as KoinSingle
                class Foo
            """)
            assertThat(ctx.resolveFqn("Single")).isEmpty()
        }
    }

    @Nested
    inner class FqnInName {
        @Test
        fun `FQN passed directly is returned as-is`() {
            val ctx = ctx("package com.example\nclass Foo")
            assertThat(ctx.resolveFqn("org.koin.core.annotation.Single"))
                .containsExactly("org.koin.core.annotation.Single")
        }
    }

    @Nested
    inner class EmptyFile {
        @Test
        fun `empty file returns empty set`() {
            val ctx = ctx("")
            assertThat(ctx.resolveFqn("Single")).isEmpty()
        }

        @Test
        fun `empty file has empty filePackage`() {
            val ctx = ctx("")
            assertThat(ctx.filePackage).isEmpty()
        }
    }

    @Nested
    inner class SamePackage {
        @Test
        fun `includes same-package candidate when no import found`() {
            val ctx = ctx("""
                package org.koin.core.annotation
                class Foo
            """)
            assertThat(ctx.resolveFqn("Single"))
                .contains("org.koin.core.annotation.Single")
        }

        @Test
        fun `exact import wins over same-package`() {
            val ctx = ctx("""
                package org.koin.core.annotation
                import javax.inject.Single
                class Foo
            """)
            // exact import takes priority — only javax result
            assertThat(ctx.resolveFqn("Single"))
                .containsExactly("javax.inject.Single")
        }
    }

    @Nested
    inner class EmptySentinel {
        @Test
        fun `EMPTY sentinel returns empty set`() {
            assertThat(FileImportContext.EMPTY.resolveFqn("Single")).isEmpty()
        }

        @Test
        fun `EMPTY sentinel filePackage is empty`() {
            assertThat(FileImportContext.EMPTY.filePackage).isEmpty()
        }

        @Test
        fun `EMPTY sentinel hasStarImportFrom returns false`() {
            assertThat(FileImportContext.EMPTY.hasStarImportFrom("org.koin")).isFalse()
        }
    }

    // --- helpers ---
    private fun ctx(code: String) = FileImportContext(compileContentForTest(code.trimIndent()))
}
