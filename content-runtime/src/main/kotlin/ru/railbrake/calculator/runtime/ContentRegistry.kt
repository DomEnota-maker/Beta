package ru.railbrake.calculator.runtime

import ru.railbrake.calculator.domain.CanonicalId
import ru.railbrake.calculator.domain.ContentEntry
import ru.railbrake.calculator.domain.ContentPackManifest
import ru.railbrake.calculator.domain.ContentTarget
import ru.railbrake.calculator.domain.RuntimeContext

data class ContentPack(
    val manifest: ContentPackManifest,
    val entries: List<ContentEntry>,
)

enum class ValidationIssueCode {
    MANIFEST_ENTRY_MISMATCH,
    DUPLICATE_CANONICAL_ID,
    DUPLICATE_ALIAS,
    ALIAS_COLLIDES_WITH_CANONICAL_ID,
    MISSING_LINK_TARGET,
}

data class ValidationIssue(
    val code: ValidationIssueCode,
    val message: String,
)

sealed interface ResolveResult {
    data class Found(val entry: ContentEntry) : ResolveResult
    data object NotFound : ResolveResult
    data object TypeMismatch : ResolveResult
    data object Inapplicable : ResolveResult
    data object LayerDenied : ResolveResult
}

class ContentRegistry {
    private val entries = linkedMapOf<CanonicalId, ContentEntry>()
    private val aliases = linkedMapOf<CanonicalId, CanonicalId>()

    fun install(pack: ContentPack): List<ValidationIssue> {
        val issues = validatePack(pack).toMutableList()

        for (entry in pack.entries) {
            if (entry.id in entries) {
                issues += ValidationIssue(
                    ValidationIssueCode.DUPLICATE_CANONICAL_ID,
                    "Canonical id already registered: ${entry.id.value}",
                )
            }
            if (entry.id in aliases) {
                issues += ValidationIssue(
                    ValidationIssueCode.ALIAS_COLLIDES_WITH_CANONICAL_ID,
                    "Canonical id collides with alias: ${entry.id.value}",
                )
            }

            for (alias in entry.aliases) {
                if (alias in entries || alias in aliases || pack.entries.any { it.id == alias }) {
                    issues += ValidationIssue(
                        ValidationIssueCode.DUPLICATE_ALIAS,
                        "Alias is not globally unique: ${alias.value}",
                    )
                }
            }
        }

        if (issues.isNotEmpty()) return issues

        for (entry in pack.entries) {
            entries[entry.id] = entry
            entry.aliases.forEach { alias -> aliases[alias] = entry.id }
        }
        return emptyList()
    }

    fun validateGlobalLinks(): List<ValidationIssue> {
        val known = entries.keys
        return entries.values.flatMap { entry ->
            entry.links
                .filter { it.targetId !in known && it.targetId !in aliases }
                .map { link ->
                    ValidationIssue(
                        ValidationIssueCode.MISSING_LINK_TARGET,
                        "${entry.id.value} -> missing ${link.targetId.value}",
                    )
                }
        }
    }

    fun resolve(target: ContentTarget, context: RuntimeContext): ResolveResult {
        val canonical = when {
            target.id in entries -> target.id
            target.id in aliases -> aliases.getValue(target.id)
            else -> return ResolveResult.NotFound
        }

        val entry = entries.getValue(canonical)
        if (entry.type != target.expectedType) return ResolveResult.TypeMismatch
        if (!entry.applicability.matches(context)) return ResolveResult.Inapplicable
        if (entry.layer !in context.allowedLayers) return ResolveResult.LayerDenied
        return ResolveResult.Found(entry)
    }

    private fun validatePack(pack: ContentPack): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        val ids = pack.entries.map { it.id }
        val idSet = ids.toSet()

        if (ids.size != idSet.size) {
            issues += ValidationIssue(
                ValidationIssueCode.DUPLICATE_CANONICAL_ID,
                "Pack contains duplicate canonical ids: ${pack.manifest.packId.value}",
            )
        }

        if (pack.manifest.entries != idSet) {
            issues += ValidationIssue(
                ValidationIssueCode.MANIFEST_ENTRY_MISMATCH,
                "Manifest entry set does not match pack content: ${pack.manifest.packId.value}",
            )
        }

        val aliasesInPack = mutableSetOf<CanonicalId>()
        for (entry in pack.entries) {
            for (alias in entry.aliases) {
                if (alias in idSet || !aliasesInPack.add(alias)) {
                    issues += ValidationIssue(
                        ValidationIssueCode.DUPLICATE_ALIAS,
                        "Alias is ambiguous inside pack: ${alias.value}",
                    )
                }
            }
        }

        return issues
    }
}
