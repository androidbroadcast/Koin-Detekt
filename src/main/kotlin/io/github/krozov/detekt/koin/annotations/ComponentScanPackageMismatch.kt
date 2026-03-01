package io.github.krozov.detekt.koin.annotations

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Detects `@Module` + `@ComponentScan("value")` where the scanned package doesn't match the module's package.
 *
 * When `@ComponentScan` specifies a package that is neither an ancestor nor a descendant of
 * the module's own package, Koin Annotations may scan the wrong package, missing components
 * or picking up unrelated ones. An empty string in `@ComponentScan("")` is also suspicious.
 *
 * <noncompliant>
 * package com.example.feature
 *
 * @Module
 * @ComponentScan("com.other.module")  // unrelated package
 * class FeatureModule
 *
 * @Module
 * @ComponentScan("")  // empty — scans nothing or current classpath root
 * class AnotherModule
 * </noncompliant>
 *
 * <compliant>
 * package com.example.feature
 *
 * @Module
 * @ComponentScan("com.example.feature")  // matches own package
 * class FeatureModule
 *
 * @Module
 * @ComponentScan  // no arg — scans current package, ok
 * class FeatureModule
 * </compliant>
 */
public class ComponentScanPackageMismatch(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "ComponentScanPackageMismatch",
        severity = Severity.Warning,
        description = "@ComponentScan package doesn't match the @Module class package",
        debt = Debt.TEN_MINS
    )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val hasModule = klass.annotationEntries.any { it.shortName?.asString() == "Module" }
        if (!hasModule) return

        val componentScan = klass.annotationEntries
            .find { it.shortName?.asString() == "ComponentScan" } ?: return

        // No argument means "scan current package" — that's fine
        val args = componentScan.valueArgumentList?.arguments
        if (args.isNullOrEmpty()) return

        val scanValue = args.firstOrNull()
            ?.getArgumentExpression()
            ?.let { it as? KtStringTemplateExpression }
            ?.entries?.joinToString("") { it.text }
            ?: return // non-string arg (e.g. constant reference) — skip

        val classPackage = klass.containingKtFile.packageFqName.asString()

        if (scanValue.isEmpty()) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(componentScan),
                    """
                    @ComponentScan("") has an empty package — scans nothing or the classpath root
                    → Specify the correct package or remove the argument to scan the current package

                    ✗ Bad:  @ComponentScan("")
                    ✓ Good: @ComponentScan  // or @ComponentScan("$classPackage")
                    """.trimIndent()
                )
            )
            return
        }

        val isRelated = classPackage.startsWith(scanValue) || scanValue.startsWith(classPackage)
        if (!isRelated) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(componentScan),
                    """
                    @ComponentScan("$scanValue") doesn't match module package '$classPackage'
                    → The scanned package is unrelated to the module — components may be missed
                    → Use a package that is an ancestor or descendant of the module's package

                    ✗ Bad:  @ComponentScan("$scanValue")  // in package $classPackage
                    ✓ Good: @ComponentScan("$classPackage")
                    """.trimIndent()
                )
            )
        }
    }
}
