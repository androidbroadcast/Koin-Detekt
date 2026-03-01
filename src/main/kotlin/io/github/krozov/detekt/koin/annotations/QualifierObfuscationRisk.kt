package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.github.krozov.detekt.koin.util.ImportAwareRule
import io.github.krozov.detekt.koin.util.Resolution
import io.github.krozov.detekt.koin.util.resolveKoin
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassLiteralExpression

/**
 * Detects `@Qualifier(SomeClass::class)` which generates a StringQualifier from the fully-qualified
 * class name, breaking after R8/ProGuard obfuscation.
 *
 * When `@Qualifier` receives a class reference, Koin Annotations generates a `StringQualifier`
 * using the FQN of that class. After obfuscation, the FQN changes, causing runtime mismatches
 * between the provider and consumer qualifiers.
 *
 * <noncompliant>
 * @Qualifier(SomeClass::class)
 * annotation class MyQualifier
 * </noncompliant>
 *
 * <compliant>
 * @Named("my-service")
 * @Single
 * class MyService
 * </compliant>
 */
internal class QualifierObfuscationRisk(config: Config = Config.empty) : ImportAwareRule(config) {
    override val issue: Issue = Issue(
        id = "QualifierObfuscationRisk",
        severity = Severity.Warning,
        description = "@Qualifier(SomeClass::class) generates a FQN-based qualifier that breaks after R8/ProGuard obfuscation",
        debt = Debt.TEN_MINS
    )

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
        super.visitAnnotationEntry(annotationEntry)

        val name = annotationEntry.shortName?.asString() ?: return
        if (name != "Qualifier") return
        if (importContext.resolveKoin(name) == Resolution.NOT_KOIN) return

        val classRef = annotationEntry.valueArgumentList?.arguments
            ?.firstOrNull { it.getArgumentExpression() is KtClassLiteralExpression }
            ?.getArgumentExpression()?.text ?: return

        report(
            CodeSmell(
                issue,
                Entity.from(annotationEntry),
                """
                @Qualifier($classRef) uses a class reference that generates a FQN-based StringQualifier
                → After R8/ProGuard obfuscation, the FQN changes and the qualifier breaks at runtime
                → Use @Named("explicit-string") instead for obfuscation-safe qualifiers

                ✗ Bad:  @Qualifier($classRef)
                ✓ Good: @Named("explicit-qualifier-name")
                """.trimIndent()
            )
        )
    }
}
