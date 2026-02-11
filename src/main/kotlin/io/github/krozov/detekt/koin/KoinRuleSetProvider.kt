package io.github.krozov.detekt.koin

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import io.github.krozov.detekt.koin.moduledsl.DeprecatedKoinApi
import io.github.krozov.detekt.koin.moduledsl.EmptyModule
import io.github.krozov.detekt.koin.moduledsl.MissingScopedDependencyQualifier
import io.github.krozov.detekt.koin.moduledsl.ModuleIncludesOrganization
import io.github.krozov.detekt.koin.moduledsl.SingleForNonSharedDependency
import io.github.krozov.detekt.koin.scope.FactoryInScopeBlock
import io.github.krozov.detekt.koin.scope.KtorRequestScopeMisuse
import io.github.krozov.detekt.koin.scope.MissingScopeClose
import io.github.krozov.detekt.koin.scope.ScopedDependencyOutsideScopeBlock
import io.github.krozov.detekt.koin.servicelocator.NoGetOutsideModuleDefinition
import io.github.krozov.detekt.koin.servicelocator.NoGlobalContextAccess
import io.github.krozov.detekt.koin.servicelocator.NoInjectDelegate
import io.github.krozov.detekt.koin.servicelocator.NoKoinComponentInterface
import io.github.krozov.detekt.koin.servicelocator.NoKoinGetInApplication

public class KoinRuleSetProvider : RuleSetProvider {

    override val ruleSetId: String = "koin-rules"

    override fun instance(config: Config): RuleSet {
        return RuleSet(
            ruleSetId,
            listOf(
                // Service Locator Anti-patterns
                NoGetOutsideModuleDefinition(config),
                NoInjectDelegate(config),
                NoKoinComponentInterface(config),
                NoGlobalContextAccess(config),
                NoKoinGetInApplication(config),
                // Module DSL Rules
                EmptyModule(config),
                SingleForNonSharedDependency(config),
                MissingScopedDependencyQualifier(config),
                DeprecatedKoinApi(config),
                ModuleIncludesOrganization(config),
                // Scope Management Rules
                MissingScopeClose(config),
                ScopedDependencyOutsideScopeBlock(config),
                FactoryInScopeBlock(config),
                KtorRequestScopeMisuse(config),
            )
        )
    }
}
