# Performance Benchmarks

This document contains baseline performance metrics for the detekt-koin rule set.

## Benchmark Environment

- **JMH Version:** 1.36
- **JVM:** OpenJDK 21.0.5 64-Bit Server VM
- **Benchmark Mode:** Average time per operation
- **Warmup:** 2 iterations, 10s each
- **Measurement:** 3 iterations, 10s each
- **Forks:** 2

## Benchmark Results

### All Rules (29 rules)

Measures the time to run all 29 rules on a typical Koin code snippet.

```
Benchmark: RuleBenchmark.benchmarkAllRules
Mode:      Average time
Score:     1.192 ± 0.026 ms/op
```

**Per-rule average:** ~0.041 ms/rule (1.192 ms / 29 rules)

### Service Locator Rules (5 rules)

Measures the time to run only the NoKoin* service locator anti-pattern rules.

```
Benchmark: RuleBenchmark.benchmarkServiceLocatorRules
Mode:      Average time
Score:     0.085 ± 0.001 ms/op
```

**Per-rule average:** ~0.017 ms/rule (0.085 ms / 5 rules)

## Performance Analysis

### Overall Performance

The rule set demonstrates excellent performance characteristics:

- **Total overhead:** ~1.2ms for all 29 rules on a typical code file
- **Per-rule overhead:** ~0.041ms average
- **Well within target:** The <10ms per rule target is met with significant headroom

### Rule Categories Performance

Service locator rules (which focus on detecting KoinComponent and inject usage) are particularly fast at ~0.017ms per rule, as they primarily perform simple pattern matching.

### Scaling Characteristics

For a typical project:
- **Small file (50 lines):** ~1.2ms analysis time
- **Medium file (200 lines):** Expected ~2-3ms (scales sub-linearly)
- **Large file (1000 lines):** Expected ~5-8ms (scales sub-linearly)

The sub-linear scaling occurs because:
1. Detekt's AST parsing is amortized across all rules
2. Most rules short-circuit on non-matching code patterns
3. The Kotlin compiler's efficient AST representation

## Performance Targets

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| Per-rule average | <10ms | ~0.041ms | ✅ Pass (244x better) |
| All rules on typical file | <300ms | ~1.2ms | ✅ Pass (250x better) |
| Memory overhead | <100MB | Not measured | N/A |

## Test Code

The benchmarks use the following representative Koin code:

```kotlin
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
```

This code exercises:
- Module DSL rules (module, single, factory, get)
- Service locator rules (KoinComponent, inject)
- Common patterns found in real Koin applications

## Running Benchmarks

To run the benchmarks yourself:

```bash
./gradlew jmh
```

Results are saved to: `build/results/jmh/results.txt`

## Future Optimizations

While current performance is excellent, potential optimizations for future versions:

1. **Caching:** Cache compiled regexes and patterns across invocations
2. **Parallel execution:** Leverage multi-core for large codebases (Detekt feature)
3. **Incremental analysis:** Only re-analyze changed files (IDE/build tool feature)
4. **AST visitor optimization:** Combine multiple rules into shared visitors

## Baseline History

### v0.4.0 (2026-02-13)

- Initial JMH benchmarking infrastructure
- Baseline: 1.192 ± 0.026 ms/op for all 29 rules
- Environment: OpenJDK 21.0.5, macOS

Future versions will track performance changes against this baseline.
