package ru.railbrake.calculator.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.railbrake.calculator.domain.ActionDisposition
import ru.railbrake.calculator.domain.ProvenanceClass
import ru.railbrake.calculator.domain.SourceStatus

class SourcePolicyTest {
    @Test
    fun conditionalActionFailsClosedForUnknownSource() {
        val decision = SourcePolicy.evaluate(
            PolicyInput(
                provenance = ProvenanceClass.CURRENT_OFFICIAL,
                sourceStatus = SourceStatus.UNKNOWN,
                actionDisposition = ActionDisposition.CONDITIONAL_ACTION,
                profileConfirmed = true,
                sourceConfirmed = true,
                safetyGateConfirmed = true,
            )
        )

        assertTrue(decision is PolicyDecision.ConditionalAction)
        assertFalse((decision as PolicyDecision.ConditionalAction).allowed)
    }

    @Test
    fun conditionalActionRequiresAllGates() {
        val decision = SourcePolicy.evaluate(
            PolicyInput(
                provenance = ProvenanceClass.CURRENT_OFFICIAL,
                sourceStatus = SourceStatus.CURRENT,
                actionDisposition = ActionDisposition.CONDITIONAL_ACTION,
                profileConfirmed = true,
                sourceConfirmed = true,
                safetyGateConfirmed = true,
            )
        )

        assertTrue((decision as PolicyDecision.ConditionalAction).allowed)
    }
}
