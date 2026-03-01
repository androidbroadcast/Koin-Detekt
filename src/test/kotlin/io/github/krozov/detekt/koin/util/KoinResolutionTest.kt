package io.github.krozov.detekt.koin.util

import io.github.detekt.test.utils.compileContentForTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KoinResolutionTest {

    @Nested
    inner class FqnInCode {
        @Test
        fun `FQN from org-koin resolves to KOIN`() {
            val ctx = ctx("package com.example\nclass Foo")
            assertThat(ctx.resolveKoin("org.koin.core.annotation.Single"))
                .isEqualTo(Resolution.KOIN)
        }

        @Test
        fun `FQN from javax-inject resolves to NOT_KOIN`() {
            val ctx = ctx("package com.example\nclass Foo")
            assertThat(ctx.resolveKoin("javax.inject.Single"))
                .isEqualTo(Resolution.NOT_KOIN)
        }
    }

    @Nested
    inner class ExactImports {
        @Test
        fun `exact import from org-koin resolves to KOIN`() {
            val ctx = ctx("""
                package com.example
                import org.koin.core.annotation.Single
                class Foo
            """)
            assertThat(ctx.resolveKoin("Single")).isEqualTo(Resolution.KOIN)
        }

        @Test
        fun `exact import from javax-inject resolves to NOT_KOIN`() {
            val ctx = ctx("""
                package com.example
                import javax.inject.Single
                class Foo
            """)
            assertThat(ctx.resolveKoin("Single")).isEqualTo(Resolution.NOT_KOIN)
        }

        @Test
        fun `alias import from org-koin resolves to KOIN via alias`() {
            val ctx = ctx("""
                package com.example
                import org.koin.core.annotation.Single as KoinSingle
                class Foo
            """)
            assertThat(ctx.resolveKoin("KoinSingle")).isEqualTo(Resolution.KOIN)
        }
    }

    @Nested
    inner class StarImports {
        @Test
        fun `koin star + name in KoinSymbols resolves to KOIN`() {
            val ctx = ctx("""
                package com.example
                import org.koin.core.annotation.*
                class Foo
            """)
            assertThat(ctx.resolveKoin("Single")).isEqualTo(Resolution.KOIN)
        }

        @Test
        fun `koin star + name NOT in KoinSymbols resolves to UNKNOWN`() {
            val ctx = ctx("""
                package com.example
                import org.koin.core.annotation.*
                class Foo
            """)
            assertThat(ctx.resolveKoin("CustomAnnotation")).isEqualTo(Resolution.UNKNOWN)
        }

        @Test
        fun `non-koin star + Single resolves to UNKNOWN`() {
            val ctx = ctx("""
                package com.example
                import javax.inject.*
                class Foo
            """)
            assertThat(ctx.resolveKoin("Single")).isEqualTo(Resolution.UNKNOWN)
        }

        @Test
        fun `koin star + exact non-koin import = NOT_KOIN (exact wins)`() {
            val ctx = ctx("""
                package com.example
                import org.koin.core.annotation.*
                import javax.inject.Single
                class Foo
            """)
            assertThat(ctx.resolveKoin("Single")).isEqualTo(Resolution.NOT_KOIN)
        }

        @Test
        fun `two stars koin and javax + Single resolves to KOIN`() {
            val ctx = ctx("""
                package com.example
                import org.koin.core.annotation.*
                import javax.inject.*
                class Foo
            """)
            // koin star + name in KoinSymbols → KOIN
            assertThat(ctx.resolveKoin("Single")).isEqualTo(Resolution.KOIN)
        }
    }

    @Nested
    inner class SamePackage {
        @Test
        fun `file in koin package resolves to KOIN`() {
            val ctx = ctx("""
                package org.koin.core.annotation
                class Foo
            """)
            assertThat(ctx.resolveKoin("Single")).isEqualTo(Resolution.KOIN)
        }

        @Test
        fun `file in non-koin package with no import resolves to UNKNOWN`() {
            val ctx = ctx("""
                package com.example
                class Foo
            """)
            assertThat(ctx.resolveKoin("Single")).isEqualTo(Resolution.UNKNOWN)
        }
    }

    @Nested
    inner class NoContext {
        @Test
        fun `empty file resolves to UNKNOWN`() {
            val ctx = ctx("")
            assertThat(ctx.resolveKoin("Single")).isEqualTo(Resolution.UNKNOWN)
        }

        @Test
        fun `EMPTY sentinel resolves to UNKNOWN`() {
            assertThat(FileImportContext.EMPTY.resolveKoin("Single"))
                .isEqualTo(Resolution.UNKNOWN)
        }
    }

    private fun ctx(code: String) = FileImportContext(compileContentForTest(code.trimIndent()))
}
