package ru.railbrake.calculator.policy

import ru.railbrake.calculator.domain.ActionDisposition
import ru.railbrake.calculator.domain.ProvenanceClass
import ru.railbrake.calculator.domain.SourceStatus

data class PolicyInput(
    val provenance: ProvenanceClass,
    val sourceStatus: SourceStatus,
    val actionDisposition: ActionDisposition,
    val profileConfirmed: Boolean,
    val sourceConfirmed: Boolean,
    val safetyGateConfirmed: Boolean,
)

sealed interface PolicyDecision {
    data object InformationOnly : PolicyDecision
    data object Prohibited : PolicyDecision
    data class ConditionalAction(val allowed: Boolean, val reason: String) : PolicyDecision
}

object SourcePolicy {
    fun evaluate(input: PolicyInput): PolicyDecision {
        if (input.actionDisposition == ActionDisposition.PROHIBITED) {
            return PolicyDecision.Prohibited
        }

        if (input.actionDisposition == ActionDisposition.INFORMATION_ONLY) {
            return PolicyDecision.InformationOnly
        }

        if (input.provenance == ProvenanceClass.UNKNOWN || input.sourceStatus == SourceStatus.UNKNOWN) {
            return PolicyDecision.ConditionalAction(false, "unknown provenance or source status")
        }

        if (input.sourceStatus != SourceStatus.CURRENT) {
            return PolicyDecision.ConditionalAction(false, "source is not current")
        }

        if (!input.sourceConfirmed) {
            return PolicyDecision.ConditionalAction(false, "source is not confirmed")
        }

        if (!input.profileConfirmed) {
            return PolicyDecision.ConditionalAction(false, "profile or variant is not confirmed")
        }

        if (!input.safetyGateConfirmed) {
            return PolicyDecision.ConditionalAction(false, "safety gate is not confirmed")
        }

        return PolicyDecision.ConditionalAction(true, "all required gates confirmed")
    }
}
