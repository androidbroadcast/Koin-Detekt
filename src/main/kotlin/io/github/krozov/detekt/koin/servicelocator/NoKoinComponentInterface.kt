package io.github.krozov.detekt.koin.servicelocator

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtObjectDeclaration

internal class NoKoinComponentInterface(config: Config) : Rule(config) {

    override val issue: Issue = Issue(
        id = "NoKoinComponentInterface",
        severity = Severity.Warning,
        description = "Detects KoinComponent or KoinScopeComponent implementation in classes " +
                "that are not framework entry points. Use constructor injection instead.",
        debt = Debt.TWENTY_MINS
    )

    private val allowedSuperTypes: List<String> by config(
        listOf(
            "Application",
            "Activity",
            "ComponentActivity",
            "Fragment",
            "Service",
            "BroadcastReceiver",
            "ViewModel"
        )
    )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        checkKoinComponent(klass.superTypeListEntries.mapNotNull { it.text }, klass.name, klass)
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
        super.visitObjectDeclaration(declaration)
        checkKoinComponent(declaration.superTypeListEntries.mapNotNull { it.text }, declaration.name, declaration)
    }

    private fun checkKoinComponent(superTypes: List<String>, name: String?, element: org.jetbrains.kotlin.psi.KtClassOrObject) {
        val koinInterface = superTypes.firstOrNull {
            it.contains("KoinComponent") || it.contains("KoinScopeComponent")
        }

        if (koinInterface == null) return

        val hasAllowedSuperType = superTypes.any { superType ->
            val shortTypeName = superType
                .substringBefore("<")
                .substringBefore("(")
                .substringAfterLast(".")
                .trim()
            allowedSuperTypes.any { allowed -> shortTypeName == allowed }
        }

        if (!hasAllowedSuperType) {
            val interfaceName = when {
                koinInterface.contains("KoinScopeComponent") -> "KoinScopeComponent"
                else -> "KoinComponent"
            }
            report(
                CodeSmell(
                    issue,
                    Entity.from(element),
                    """
                    $interfaceName interface found → Breaks dependency inversion, harder to test
                    → Use constructor injection instead

                    ✗ Bad:  class MyRepo : KoinComponent { val api = get<Api>() }
                    ✓ Good: class MyRepo(private val api: Api)
                    """.trimIndent()
                )
            )
        }
    }
}
