package io.github.krozov.detekt.koin.benchmarks

import io.github.krozov.detekt.koin.KoinRuleSetProvider
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
open class RuleBenchmark {

    private lateinit var testCode: String
    private lateinit var ruleSet: KoinRuleSetProvider

    @Setup
    fun setup() {
        ruleSet = KoinRuleSetProvider()
        testCode = """
            import org.koin.dsl.module
            import org.koin.core.component.KoinComponent

            val appModule = module {
                single { ApiService() }
                single { Repository(get()) }
                factory { UseCase(get()) }
            }

            class MyClass : KoinComponent {
                val api by inject<ApiService>()
            }
        """.trimIndent()
    }

    @Benchmark
    fun benchmarkAllRules() {
        val rules = ruleSet.instance(Config.empty).rules
        rules.forEach { rule ->
            rule.lint(testCode)
        }
    }

    @Benchmark
    fun benchmarkServiceLocatorRules() {
        val rules = ruleSet.instance(Config.empty).rules
            .filter { it.ruleId.startsWith("NoKoin") }
        rules.forEach { rule ->
            rule.lint(testCode)
        }
    }
}
