package io.github.krozov.detekt.koin.util

/**
 * Strips nullable marker (`?`) and type parameters (`<…>`) from a type text,
 * returning the bare outer type name.
 *
 * Examples:
 * - `"Single"` → `"Single"`
 * - `"Single?"` → `"Single"`
 * - `"Lazy<Single>"` → `"Lazy"`
 * - `"Lazy<Single>?"` → `"Lazy"`
 * - `"Map<String, Int>"` → `"Map"`
 */
internal fun stripTypeMetadata(typeText: String): String =
    typeText.substringBefore('<').trimEnd('?').trim()

/**
 * Returns the text inside the outermost `<…>` of a generic type, or null if not generic.
 *
 * Examples:
 * - `"Lazy<Single>"` → `"Single"`
 * - `"Map<String, Int>"` → `"String, Int"`
 * - `"Single"` → null
 * - `"Lazy<List<Int>>"` → `"List<Int>"` (inner content as-is)
 */
internal fun typeArgumentsText(typeText: String): String? {
    val start = typeText.indexOf('<')
    if (start == -1) return null
    val end = typeText.lastIndexOf('>')
    if (end <= start) return null
    return typeText.substring(start + 1, end).trim()
}
