package ru.railbrake.calculator.diagnostics

import java.util.Locale
import ru.railbrake.calculator.domain.CanonicalId
import ru.railbrake.calculator.domain.ContentOwner
import ru.railbrake.calculator.domain.ContentTarget
import ru.railbrake.calculator.domain.ContentType
import ru.railbrake.calculator.domain.ModelId
import ru.railbrake.calculator.domain.RuntimeContext
import ru.railbrake.calculator.runtime.ContentRegistry
import ru.railbrake.calculator.runtime.ResolveResult

/**
 * The executable donor graph is NOT a publication decision.
 *
 * Only independently accepted canonical content may enter the end-user catalog.
 * Registry.resolve additionally enforces variant applicability and allowed layers.
 */
class PublishedDiagnosticCatalog(
    private val runtime: ExecutableDiagnosticRuntime,
    private val registry: ContentRegistry,
    private val context: RuntimeContext,
) {
    private val owner = ContentOwner.Model(ModelId(runtime.modelId))

    fun approvedScenarios(): List<ExecutableDiagnosticScenario> {
        if (context.contentModelId != ModelId(runtime.modelId)) return emptyList()

        return runtime.scenarios.filter { scenario ->
            val target = ContentTarget(
                id = CanonicalId(scenario.id),
                expectedType = ContentType.DIAGNOSTIC_SCENARIO,
            )
            val resolved = registry.resolve(target, context)
            resolved is ResolveResult.Found &&
                resolved.entry.owner == owner &&
                resolved.entry.id == target.id
        }
    }

    fun search(query: String): List<ExecutableDiagnosticScenario> {
        val needle = query.trim().lowercase(Locale.ROOT)
        val published = approvedScenarios()
        if (needle.isBlank()) return published

        return published.filter { scenario ->
            sequenceOf(
                scenario.title,
                scenario.summary,
                scenario.category,
                scenario.applicability,
            ).plus(scenario.observableSigns.asSequence())
                .any { needle in it.lowercase(Locale.ROOT) }
        }
    }

    /** Never provide a candidate graph to an interactive end-user screen. */
    fun open(canonicalId: String): ExecutableDiagnosticScenario? =
        approvedScenarios().firstOrNull { it.id == canonicalId }
}
