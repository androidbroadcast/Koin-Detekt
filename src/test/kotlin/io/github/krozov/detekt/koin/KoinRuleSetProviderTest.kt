package io.github.krozov.detekt.koin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KoinRuleSetProviderTest {

    @Test
    fun `should have correct rule set id`() {
        val provider = KoinRuleSetProvider()
        assertThat(provider.ruleSetId).isEqualTo("koin-rules")
    }

    @Test
    fun `should provide rules`() {
        val provider = KoinRuleSetProvider()
        val config = io.gitlab.arturbosch.detekt.api.Config.empty
        val ruleSet = provider.instance(config)

        assertThat(ruleSet.rules).isNotEmpty()
        assertThat(ruleSet.rules).hasSize(31)
    }
}
