package io.github.krozov.detekt.koin.util

import org.jetbrains.kotlin.psi.KtAnnotated

/**
 * Returns the first annotation short name from [allowed] that is not confirmed as non-Koin.
 * [Resolution.UNKNOWN] is treated as potentially Koin (backwards-compatible).
 */
internal fun KtAnnotated.firstKoinAnnotationName(
    ctx: FileImportContext,
    allowed: Set<String>,
): String? = annotationEntries
    .mapNotNull { it.shortName?.asString() }
    .firstOrNull { it in allowed && ctx.resolveKoin(it) != Resolution.NOT_KOIN }

/**
 * Returns true if any annotation from [allowed] is not confirmed as non-Koin.
 */
internal fun KtAnnotated.hasKoinAnnotationFrom(
    ctx: FileImportContext,
    allowed: Set<String>,
): Boolean = firstKoinAnnotationName(ctx, allowed) != null

/**
 * Returns all annotation short names from [allowed] that are not confirmed as non-Koin.
 */
internal fun KtAnnotated.koinAnnotationNames(
    ctx: FileImportContext,
    allowed: Set<String>,
): List<String> = annotationEntries
    .mapNotNull { it.shortName?.asString() }
    .filter { it in allowed && ctx.resolveKoin(it) != Resolution.NOT_KOIN }
