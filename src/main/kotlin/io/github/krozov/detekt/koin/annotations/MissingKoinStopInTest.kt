package io.github.krozov.detekt.koin.annotations

import io.github.krozov.detekt.koin.util.value
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Detects test classes that call `startKoin` without a corresponding `stopKoin()` in `@After`/`@AfterEach`.
 *
 * Without `stopKoin()` in a teardown method, subsequent tests will fail with
 * `KoinApplicationAlreadyStartedException` because Koin remains started from the previous test.
 *
 * <noncompliant>
 * class MyTest {
 *     @Before fun setup() { startKoin { modules(m) } }
 *     // No stopKoin() in @After — next test will crash
 * }
 * </noncompliant>
 *
 * <compliant>
 * class MyTest {
 *     @Before fun setup() { startKoin { modules(m) } }
 *     @After fun teardown() { stopKoin() }
 * }
 * </compliant>
 */
internal class MissingKoinStopInTest(config: Config = Config.empty) : Rule(config) {
    override val issue: Issue = Issue(
        id = "MissingKoinStopInTest",
        severity = Severity.Warning,
        description = "startKoin() in test without stopKoin() in @After causes KoinApplicationAlreadyStartedException",
        debt = Debt.FIVE_MINS
    )

    private val additionalTeardownAnnotations: List<String> =
        config.value(key = "additionalTeardownAnnotations", default = emptyList())

    private val teardownAnnotations: Set<String> =
        (listOf("After", "AfterEach", "AfterAll") + additionalTeardownAnnotations).toSet()

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val functions = klass.body?.functions ?: return

        // Check if class calls startKoin anywhere (in any method)
        val hasStartKoin = klass.collectDescendantsOfType<KtCallExpression>()
            .any { it.calleeExpression?.text == "startKoin" }
        if (!hasStartKoin) return

        // Check if any @After/@AfterEach method calls stopKoin()
        val hasTeardownWithStopKoin = functions.any { function ->
            val isTeardown = function.annotationEntries
                .any { it.shortName?.asString() in teardownAnnotations }
            if (!isTeardown) return@any false

            function.collectDescendantsOfType<KtCallExpression>()
                .any { it.calleeExpression?.text == "stopKoin" }
        }

        if (!hasTeardownWithStopKoin) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(klass),
                    """
                    Test class ${klass.name} calls startKoin() but has no stopKoin() in @After/@AfterEach
                    -> Without stopKoin(), subsequent tests fail with KoinApplicationAlreadyStartedException

                    Bad:  @Before fun setup() { startKoin { modules(m) } }  // no @After with stopKoin()
                    Good: @After fun teardown() { stopKoin() }
                    """.trimIndent()
                )
            )
        }
    }
}
