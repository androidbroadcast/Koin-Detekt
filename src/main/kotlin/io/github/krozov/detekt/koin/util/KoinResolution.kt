package io.github.krozov.detekt.koin.util

/**
 * Resolves whether [name] refers to a Koin type using import information from this context.
 *
 * - [Resolution.KOIN]     — unambiguously a Koin type
 * - [Resolution.NOT_KOIN] — unambiguously not a Koin type
 * - [Resolution.UNKNOWN]  — cannot be determined; each rule decides how to handle this
 * @param name Simple name, alias, or FQN. Must not include type parameters — strip `<…>` before calling.
 */
internal fun FileImportContext.resolveKoin(name: String): Resolution {
    val candidates = resolveFqn(name)

    // resolveFqn() uses a same-package heuristic when exactImports is empty:
    // it returns "$filePackage.$name" as a candidate. This heuristic has two limitations:
    //
    // 1. When star imports are also present, the name may come from a star-imported package
    //    rather than the file's own package — strip the same-package candidate in that case.
    //
    // 2. When the same-package candidate is NOT a Koin FQN, we cannot conclude the name is
    //    NOT_KOIN: the definition may be elsewhere (e.g. imported transitively). Return UNKNOWN.
    //    Only a same-package Koin FQN is reliable enough to conclude KOIN.
    val samePkgCandidate = if (filePackage.isNotEmpty()) "$filePackage.$name" else null
    val exactCandidates = if (samePkgCandidate != null) {
        candidates - samePkgCandidate
    } else {
        candidates
    }

    // Exact / alias / FQN candidates (not from same-package heuristic)
    if (exactCandidates.isNotEmpty()) {
        val hasKoin    = exactCandidates.any { fqn -> isKoinFqn(fqn) }
        val hasNonKoin = exactCandidates.any { fqn -> !isKoinFqn(fqn) }
        return when {
            hasKoin && !hasNonKoin  -> Resolution.KOIN
            hasNonKoin && !hasKoin  -> Resolution.NOT_KOIN
            else                    -> Resolution.UNKNOWN  // ambiguous
        }
    }

    // Only same-package candidate remains (no explicit imports at all)
    if (samePkgCandidate != null && candidates.contains(samePkgCandidate) && !hasAnyStarImport) {
        // Same-package is only trustworthy for Koin packages: if the file lives inside
        // a Koin package, the name is likely Koin. Non-Koin same-package → UNKNOWN.
        if (isKoinFqn(samePkgCandidate)) return Resolution.KOIN
        return Resolution.UNKNOWN
    }

    // No exact/same-package candidate — check star imports
    val koinStarPresent = KoinSymbols.KOIN_PACKAGES.any { hasStarImportFrom(it) }
    if (koinStarPresent) {
        return if (KoinSymbols.isKnownName(name)) Resolution.KOIN else Resolution.UNKNOWN
    }

    return Resolution.UNKNOWN
}

private fun isKoinFqn(fqn: String): Boolean =
    KoinSymbols.KOIN_PACKAGES.any { pkg ->
        fqn == pkg || fqn.startsWith("$pkg.")
    }
