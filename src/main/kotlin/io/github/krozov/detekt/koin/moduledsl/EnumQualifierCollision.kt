package io.github.krozov.detekt.koin.moduledsl

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile

internal class EnumQualifierCollision(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "EnumQualifierCollision",
        severity = Severity.Warning,
        description = "Detects enum qualifiers with same value name from different enum types (collision risk with R8/ProGuard)",
        debt = Debt.TEN_MINS
    )

    // Track enum qualifiers: map of (enumValueName -> list of (enumTypeName, expression))
    private val enumQualifiersInModule = mutableMapOf<String, MutableList<Pair<String, KtCallExpression>>>()

    override fun visitKtFile(file: KtFile) {
        enumQualifiersInModule.clear()
        super.visitKtFile(file)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        // Reset tracking when entering a new module
        if (expression.calleeExpression?.text == "module") {
            enumQualifiersInModule.clear()
        }

        // Check for Koin definition calls with named() qualifiers
        val calleeName = expression.calleeExpression?.text ?: return
        if (calleeName !in setOf("single", "factory", "scoped", "viewModel", "worker")) return

        // Look for named(EnumType.VALUE) patterns in arguments
        expression.valueArguments.forEach { arg ->
            val argText = arg.getArgumentExpression()?.text ?: return@forEach

            // Match pattern: named(SomeEnum.SOME_VALUE)
            val enumPattern = Regex("""named\s*\(\s*(\w+)\.(\w+)\s*\)""")
            val match = enumPattern.find(argText) ?: return@forEach

            val enumTypeName = match.groupValues[1]
            val enumValueName = match.groupValues[2]

            // Skip string literals (they start with quotes)
            if (enumTypeName.startsWith("\"")) return@forEach

            // Track this enum qualifier
            val existing = enumQualifiersInModule.getOrPut(enumValueName) { mutableListOf() }

            // Check if we have a different enum type with same value name
            val hasDifferentType = existing.any { (existingType, _) -> existingType != enumTypeName }

            if (hasDifferentType) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(expression),
                        """
                        Enum qualifier collision: '$enumValueName' used by multiple enum types
                        → R8/ProGuard may treat these as identical, causing runtime errors
                        → Use string qualifiers or ensure unique enum value names

                        ✗ Bad:  enum class Type1 { VALUE }; enum class Type2 { VALUE }
                                single(named(Type1.VALUE)) { ... }
                                single(named(Type2.VALUE)) { ... }
                        ✓ Good: single(named("type1_value")) { ... }
                                single(named("type2_value")) { ... }
                        """.trimIndent()
                    )
                )
            }

            existing.add(enumTypeName to expression)
        }
    }
}
