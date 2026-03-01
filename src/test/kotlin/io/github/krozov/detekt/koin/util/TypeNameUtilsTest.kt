package io.github.krozov.detekt.koin.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TypeNameUtilsTest {

    @Nested
    inner class StripTypeMetadata {
        @Test
        fun `simple name unchanged`() {
            assertThat(stripTypeMetadata("Single")).isEqualTo("Single")
        }

        @Test
        fun `nullable simple name stripped`() {
            assertThat(stripTypeMetadata("Single?")).isEqualTo("Single")
        }

        @Test
        fun `generic type returns outer name`() {
            assertThat(stripTypeMetadata("Lazy<Single>")).isEqualTo("Lazy")
        }

        @Test
        fun `nullable generic type returns outer name`() {
            assertThat(stripTypeMetadata("Lazy<Single>?")).isEqualTo("Lazy")
        }

        @Test
        fun `nested generic returns outer name`() {
            assertThat(stripTypeMetadata("Map<String, Int>")).isEqualTo("Map")
        }

        @Test
        fun `FQN without generics unchanged`() {
            assertThat(stripTypeMetadata("org.koin.core.annotation.Single"))
                .isEqualTo("org.koin.core.annotation.Single")
        }
    }

    @Nested
    inner class TypeArgumentsText {
        @Test
        fun `simple name returns null`() {
            assertThat(typeArgumentsText("Single")).isNull()
        }

        @Test
        fun `single type argument extracted`() {
            assertThat(typeArgumentsText("Lazy<Single>")).isEqualTo("Single")
        }

        @Test
        fun `multiple type arguments extracted`() {
            assertThat(typeArgumentsText("Map<String, Int>")).isEqualTo("String, Int")
        }

        @Test
        fun `nested generic returns inner content as-is`() {
            assertThat(typeArgumentsText("Lazy<List<Int>>")).isEqualTo("List<Int>")
        }

        @Test
        fun `nullable generic extracts argument`() {
            assertThat(typeArgumentsText("Lazy<Single>?")).isEqualTo("Single")
        }
    }
}
