package io.github.krozov.detekt.koin

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import io.github.krozov.detekt.koin.moduledsl.ConstructorDslAmbiguousParameters
import io.github.krozov.detekt.koin.moduledsl.DeprecatedKoinApi
import io.github.krozov.detekt.koin.moduledsl.DuplicateBindingWithoutQualifier
import io.github.krozov.detekt.koin.moduledsl.EmptyModule
import io.github.krozov.detekt.koin.moduledsl.EnumQualifierCollision
import io.github.krozov.detekt.koin.moduledsl.ExcessiveCreatedAtStart
import io.github.krozov.detekt.koin.moduledsl.GenericDefinitionWithoutQualifier
import io.github.krozov.detekt.koin.moduledsl.MissingScopedDependencyQualifier
import io.github.krozov.detekt.koin.moduledsl.ModuleAsTopLevelVal
import io.github.krozov.detekt.koin.moduledsl.ModuleIncludesOrganization
import io.github.krozov.detekt.koin.moduledsl.OverrideInIncludedModule
import io.github.krozov.detekt.koin.moduledsl.ParameterTypeMatchesReturnType
import io.github.krozov.detekt.koin.moduledsl.SingleForNonSharedDependency
import io.github.krozov.detekt.koin.moduledsl.UnassignedQualifierInWithOptions
import io.github.krozov.detekt.koin.scope.CloseableWithoutOnClose
import io.github.krozov.detekt.koin.scope.FactoryInScopeBlock
import io.github.krozov.detekt.koin.scope.KtorRequestScopeMisuse
import io.github.krozov.detekt.koin.scope.MissingScopeClose
import io.github.krozov.detekt.koin.scope.ScopeAccessInOnDestroy
import io.github.krozov.detekt.koin.scope.ScopeDeclareWithActivityOrFragment
import io.github.krozov.detekt.koin.scope.ScopedDependencyOutsideScopeBlock
import io.github.krozov.detekt.koin.scope.ViewModelAsSingleton
import io.github.krozov.detekt.koin.servicelocator.NoGetOutsideModuleDefinition
import io.github.krozov.detekt.koin.servicelocator.NoGlobalContextAccess
import io.github.krozov.detekt.koin.servicelocator.NoInjectDelegate
import io.github.krozov.detekt.koin.servicelocator.NoKoinComponentInterface
import io.github.krozov.detekt.koin.servicelocator.NoKoinGetInApplication
// Platform rules - Compose
import io.github.krozov.detekt.koin.platform.compose.KoinInjectInPreview
import io.github.krozov.detekt.koin.platform.compose.KoinViewModelOutsideComposable
import io.github.krozov.detekt.koin.platform.compose.RememberKoinModulesLeak
// Platform rules - Ktor
import io.github.krozov.detekt.koin.platform.ktor.KtorApplicationKoinInit
import io.github.krozov.detekt.koin.platform.ktor.KtorRouteScopeMisuse
// Platform rules - Android
import io.github.krozov.detekt.koin.platform.android.ActivityFragmentKoinScope
import io.github.krozov.detekt.koin.platform.android.AndroidContextNotFromKoin
// Platform rules - General
import io.github.krozov.detekt.koin.platform.StartKoinInActivity
// Architecture rules
import io.github.krozov.detekt.koin.architecture.CircularModuleDependency
import io.github.krozov.detekt.koin.architecture.GetConcreteTypeInsteadOfInterface
import io.github.krozov.detekt.koin.architecture.LayerBoundaryViolation
import io.github.krozov.detekt.koin.architecture.PlatformImportRestriction
// Annotation rules
import io.github.krozov.detekt.koin.annotations.MixingDslAndAnnotations
import io.github.krozov.detekt.koin.annotations.MissingModuleAnnotation
import io.github.krozov.detekt.koin.annotations.ConflictingBindings
import io.github.krozov.detekt.koin.annotations.ScopedWithoutQualifier
import io.github.krozov.detekt.koin.annotations.AnnotationProcessorNotConfigured
import io.github.krozov.detekt.koin.annotations.SingleAnnotationOnObject
import io.github.krozov.detekt.koin.annotations.TooManyInjectedParams
import io.github.krozov.detekt.koin.annotations.InvalidNamedQualifierCharacters
import io.github.krozov.detekt.koin.annotations.KoinAnnotationOnExtensionFunction
import io.github.krozov.detekt.koin.annotations.ViewModelAnnotatedAsSingle
import io.github.krozov.detekt.koin.annotations.AnnotatedClassImplementsNestedInterface
import io.github.krozov.detekt.koin.annotations.InjectedParamWithNestedGenericType

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
                UnassignedQualifierInWithOptions(config),
                DuplicateBindingWithoutQualifier(config),
                GenericDefinitionWithoutQualifier(config),
                EnumQualifierCollision(config),
                ConstructorDslAmbiguousParameters(config),
                ParameterTypeMatchesReturnType(config),
                ExcessiveCreatedAtStart(config),
                OverrideInIncludedModule(config),
                ModuleAsTopLevelVal(config),
                // Scope Management Rules
                MissingScopeClose(config),
                ScopedDependencyOutsideScopeBlock(config),
                ViewModelAsSingleton(config),
                CloseableWithoutOnClose(config),
                ScopeAccessInOnDestroy(config),
                FactoryInScopeBlock(config),
                KtorRequestScopeMisuse(config),
                ScopeDeclareWithActivityOrFragment(config),
                // Platform Rules - Compose
                KoinViewModelOutsideComposable(config),
                KoinInjectInPreview(config),
                RememberKoinModulesLeak(config),
                // Platform Rules - Ktor
                KtorApplicationKoinInit(config),
                KtorRouteScopeMisuse(config),
                // Platform Rules - Android
                AndroidContextNotFromKoin(config),
                ActivityFragmentKoinScope(config),
                // Platform Rules - General
                StartKoinInActivity(config),
                // Architecture Rules
                LayerBoundaryViolation(config),
                PlatformImportRestriction(config),
                CircularModuleDependency(config),
                GetConcreteTypeInsteadOfInterface(config),
                // Annotation rules
                MixingDslAndAnnotations(config),
                MissingModuleAnnotation(config),
                ConflictingBindings(config),
                ScopedWithoutQualifier(config),
                AnnotationProcessorNotConfigured(config),
                // New annotation rules
                SingleAnnotationOnObject(config),
                TooManyInjectedParams(config),
                InvalidNamedQualifierCharacters(config),
                KoinAnnotationOnExtensionFunction(config),
                ViewModelAnnotatedAsSingle(config),
                AnnotatedClassImplementsNestedInterface(config),
                InjectedParamWithNestedGenericType(config)
            )
        )
    }
}
