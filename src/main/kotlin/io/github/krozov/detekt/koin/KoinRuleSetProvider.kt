package io.github.krozov.detekt.koin

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import io.github.krozov.detekt.koin.servicelocator.NoGetOutsideModuleDefinition
import io.github.krozov.detekt.koin.servicelocator.NoGlobalContextAccess
import io.github.krozov.detekt.koin.servicelocator.NoInjectDelegate
import io.github.krozov.detekt.koin.servicelocator.NoKoinComponentInterface
import io.github.krozov.detekt.koin.servicelocator.NoKoinGetInApplication

class KoinRuleSetProvider : RuleSetProvider {

    override val ruleSetId: String = "koin-rules"

    override fun instance(config: Config): RuleSet {
        return RuleSet(
            ruleSetId,
            listOf(
                NoGetOutsideModuleDefinition(config),
                NoInjectDelegate(config),
                NoKoinComponentInterface(config),
                NoGlobalContextAccess(config),
                NoKoinGetInApplication(config),
            )
        )
    }
}
