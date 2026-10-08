package ru.railbrake.calculator.atlas

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AtlasVariantPolicyTest {
    private val catalog = AtlasVariantCatalog(
        profiles = mapOf(
            "vl80s_697_1260" to AtlasVariantProfile(
                id = "vl80s_697_1260",
                confidence = "CONFIRMED_GENERAL",
                features = setOf(
                    "sme_three_section_capable",
                    "third_section_rheostatic_brake_unavailable",
                ),
                rules = emptySet(),
            ),
            "vl80s_2349_2653" to AtlasVariantProfile(
                id = "vl80s_2349_2653",
                confidence = "SECONDARY_SOURCE_REQUIRES_DRAWING",
                features = setOf(
                    "burt16",
                    "vu_protection_rp21_22",
                    "aux_compressor_pvu7",
                ),
                rules = emptySet(),
            ),
            "vl80s_unknown" to AtlasVariantProfile(
                id = "vl80s_unknown",
                confidence = "FALLBACK",
                features = emptySet(),
                rules = setOf("no_exact_scheme_assumption"),
            ),
            "vl80s_modified_or_mixed" to AtlasVariantProfile(
                id = "vl80s_modified_or_mixed",
                confidence = "PROFILE_REQUIRED",
                features = emptySet(),
                rules = setOf("actual_section_drawing_has_priority"),
            ),
        )
    )

    private val electrical = ElectricalVariantPolicy(
        overlays = listOf(
            ElectricalVariantOverlay(
                id = "VL-OVL-EL-AUX-PVU7-2349",
                title = "ПВУ7",
                triggerFeature = "aux_compressor_pvu7",
                triggerValue = true,
                serialAutoInference = true,
                appliesTo = setOf("VL-SCH-EL-AUX-937-1260"),
                action = "insert_pvu7_variant_fragment",
                status = "SECONDARY_SCHEME_CONFIRMED_PRIMARY_SECTION_DRAWING_REQUIRED",
                evidenceNote = "Нужен лист секции.",
            ),
            ElectricalVariantOverlay(
                id = "VL-OVL-EL-FIRE-2110",
                title = "Пожарная сигнализация",
                triggerFeature = "fire_signalization_present",
                triggerValue = true,
                serialAutoInference = false,
                appliesTo = setOf("VL-SCH-EL-FIRE-SIGNAL"),
                action = "enable_fire_signal_scheme",
                status = "EVIDENCE_CONFLICT_SERIAL_BOUNDARY",
                evidenceNote = "Использовать фактическое оснащение.",
            ),
        )
    )

    private val pneumatic = PneumaticVariantPolicy(
        rules = listOf(
            PneumaticVariantRule(
                "VL-PN-VAR-ACTUAL-SECTION",
                "modified_or_mixed",
                "suppress exact claims",
                "REQUIRED",
            ),
            PneumaticVariantRule(
                "VL-PN-VAR-KM395-INDEX",
                "additional KM395 position",
                "require full index",
                "REQUIRED",
            ),
            PneumaticVariantRule(
                "VL-PN-VAR-THIRD-SECTION",
                "three-section operation",
                "retain section connection",
                "CONFIRMED_GENERAL",
            ),
            PneumaticVariantRule(
                "VL-PN-VAR-COMP-882-883",
                "section 882 or 883",
                "retain override",
                "HISTORICAL_OVERRIDE",
            ),
            PneumaticVariantRule(
                "VL-PN-VAR-SAFETY-EQUIPMENT",
                "reconfigured safety complex",
                "follow actual equipment",
                "PROFILE_REQUIRED",
            ),
        ),
        stateGates = mapOf(
            "VL-BR-STATE-RHEO-INTERLOCK" to PneumaticStateGate(
                "VL-BR-STATE-RHEO-INTERLOCK",
                "SECTION_PROFILE_AND_RHEOSTATIC_CAPABILITY_REQUIRED",
            ),
        ),
    )

    private val resolver = AtlasVariantPolicyResolver(catalog, electrical, pneumatic)

    @Test
    fun secondaryElectricalOverlayIsSelectedButExactDetailStaysClosed() {
        val decision = resolver.resolveElectrical(
            schemeId = "VL-SCH-EL-AUX-937-1260",
            context = AtlasVariantContext(profileId = "vl80s_2349_2653"),
        )

        assertTrue("VL-OVL-EL-AUX-PVU7-2349" in decision.applicableOverlayIds)
        assertFalse(decision.exactDetailAllowed)
        assertTrue(decision.blockingReasons.isNotEmpty())
    }

    @Test
    fun actualSectionDrawingCanOpenExactDetailForResolvedOverlay() {
        val decision = resolver.resolveElectrical(
            schemeId = "VL-SCH-EL-AUX-937-1260",
            context = AtlasVariantContext(
                profileId = "vl80s_2349_2653",
                actualSectionDrawingConfirmed = true,
            ),
        )

        assertTrue(decision.exactDetailAllowed)
        assertTrue("VL-OVL-EL-AUX-PVU7-2349" in decision.applicableOverlayIds)
    }

    @Test
    fun fireSignalOverlayNeverUsesSerialInference() {
        val inferred = resolver.resolveElectrical(
            schemeId = "VL-SCH-EL-FIRE-SIGNAL",
            context = AtlasVariantContext(profileId = "vl80s_697_1260"),
        )
        assertFalse("VL-OVL-EL-FIRE-2110" in inferred.applicableOverlayIds)

        val explicit = resolver.resolveElectrical(
            schemeId = "VL-SCH-EL-FIRE-SIGNAL",
            context = AtlasVariantContext(
                profileId = "vl80s_697_1260",
                explicitFeatures = setOf("fire_signalization_present"),
            ),
        )
        assertTrue("VL-OVL-EL-FIRE-2110" in explicit.applicableOverlayIds)
        assertFalse(explicit.exactDetailAllowed)
    }

    @Test
    fun unknownProfileFailsClosed() {
        val decision = resolver.resolveElectrical(
            schemeId = "VL-SCH-EL-AUX-937-1260",
            context = AtlasVariantContext(profileId = "vl80s_unknown"),
        )
        assertFalse(decision.exactDetailAllowed)
    }

    @Test
    fun pneumaticRheostaticStateRequiresConfirmedCapability() {
        val denied = resolver.resolvePneumatic(
            stateId = "VL-BR-STATE-RHEO-INTERLOCK",
            context = AtlasVariantContext(profileId = "vl80s_697_1260"),
        )
        assertFalse(denied.exactDetailAllowed)

        val allowed = resolver.resolvePneumatic(
            stateId = "VL-BR-STATE-RHEO-INTERLOCK",
            context = AtlasVariantContext(
                profileId = "vl80s_697_1260",
                rheostaticCapabilityConfirmed = true,
            ),
        )
        assertTrue(allowed.exactDetailAllowed)
    }

    @Test
    fun modifiedSectionRequiresActualSectionEvidence() {
        val decision = resolver.resolvePneumatic(
            stateId = null,
            context = AtlasVariantContext(profileId = "vl80s_modified_or_mixed"),
        )
        assertFalse(decision.exactDetailAllowed)
        assertTrue("VL-PN-VAR-ACTUAL-SECTION" in decision.applicableRuleIds)
    }
}
