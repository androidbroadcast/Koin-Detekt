package io.github.krozov.detekt.koin.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ResolutionTest {
    @Test
    fun `Resolution has exactly three values`() {
        assertThat(Resolution.entries).containsExactlyInAnyOrder(
            Resolution.KOIN, Resolution.NOT_KOIN, Resolution.UNKNOWN
        )
    }
}
