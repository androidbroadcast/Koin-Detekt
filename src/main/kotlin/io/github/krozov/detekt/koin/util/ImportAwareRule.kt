package io.github.krozov.detekt.koin.util

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtFile

/**
 * Base class for rules that need import-based type resolution.
 *
 * Provides [importContext] — a per-file snapshot of the file's imports and package.
 * Call [importContext.resolveKoin(name)][resolveKoin] to get a [Resolution].
 *
 * Rules that override [visitKtFile] must call [super.visitKtFile] to ensure
 * [importContext] is updated before visiting child elements.
 */
internal abstract class ImportAwareRule(config: Config) : Rule(config) {

    var importContext: FileImportContext = FileImportContext.EMPTY
        private set

    override fun visitKtFile(file: KtFile) {
        importContext = FileImportContext(file)
        super.visitKtFile(file)
    }
}
