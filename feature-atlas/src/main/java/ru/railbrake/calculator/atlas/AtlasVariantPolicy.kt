package ru.railbrake.calculator.atlas

import com.google.gson.Gson

data class AtlasVariantProfile(
    val id: String,
    val confidence: String,
    val features: Set<String>,
    val rules: Set<String>,
)

data class AtlasVariantCatalog(
    val profiles: Map<String, AtlasVariantProfile>,
)

class AtlasVariantCatalogJsonLoader {
    private val gson = Gson()

    fun parse(json: String): AtlasVariantCatalog {
        val raw = gson.fromJson(json, JsonProfileCatalog::class.java)
            ?: error("variant profile catalog is empty")

        val profiles = raw.physicalBuckets.orEmpty().associate { item ->
            val id = requireText(item.id, "profile.id")
            id to AtlasVariantProfile(
                id = id,
                confidence = requireText(item.confidence, "profile.confidence"),
                features = item.features.orEmpty().toSet(),
                rules = item.rules.orEmpty().toSet(),
            )
        }

        require(profiles.isNotEmpty()) { "variant profile catalog is empty" }
        return AtlasVariantCatalog(profiles)
    }
}

data class ElectricalVariantOverlay(
    val id: String,
    val title: String,
    val triggerFeature: String,
    val triggerValue: Boolean,
    val serialAutoInference: Boolean,
    val appliesTo: Set<String>,
    val action: String,
    val status: String,
    val evidenceNote: String?,
)

data class ElectricalVariantPolicy(
    val overlays: List<ElectricalVariantOverlay>,
)

class ElectricalVariantPolicyJsonLoader {
    private val gson = Gson()

    fun parse(json: String): ElectricalVariantPolicy {
        val raw = gson.fromJson(json, JsonElectricalDocument::class.java)
            ?: error("electrical variant policy is empty")

        val overlays = raw.policy?.variantOverlays.orEmpty().map { item ->
            val trigger = requireNotNull(item.trigger) {
                "overlay ${item.id} requires trigger"
            }
            ElectricalVariantOverlay(
                id = requireText(item.id, "overlay.id"),
                title = requireText(item.title, "overlay.title"),
                triggerFeature = requireText(trigger.feature, "overlay.trigger.feature"),
                triggerValue = trigger.value ?: true,
                serialAutoInference = trigger.serialAutoInference ?: true,
                appliesTo = item.appliesTo.orEmpty().toSet(),
                action = requireText(item.action, "overlay.action"),
                status = requireText(item.status, "overlay.status"),
                evidenceNote = item.evidenceNote?.takeIf(String::isNotBlank),
            )
        }

        require(overlays.isNotEmpty()) { "electrical overlay list is empty" }
        return ElectricalVariantPolicy(overlays)
    }
}

data class PneumaticVariantRule(
    val id: String,
    val condition: String,
    val action: String,
    val status: String,
)

data class PneumaticStateGate(
    val id: String,
    val variantGate: String,
)

data class PneumaticVariantPolicy(
    val rules: List<PneumaticVariantRule>,
    val stateGates: Map<String, PneumaticStateGate>,
)

class PneumaticVariantPolicyJsonLoader {
    private val gson = Gson()

    fun parse(json: String): PneumaticVariantPolicy {
        val raw = gson.fromJson(json, JsonPneumaticDocument::class.java)
            ?: error("pneumatic variant policy is empty")
        val policy = requireNotNull(raw.policy) { "pneumatic policy is missing" }

        val rules = policy.variantContract?.rules.orEmpty().map { item ->
            PneumaticVariantRule(
                id = requireText(item.id, "pneumaticRule.id"),
                condition = requireText(item.condition, "pneumaticRule.condition"),
                action = requireText(item.action, "pneumaticRule.action"),
                status = requireText(item.status, "pneumaticRule.status"),
            )
        }
        val gates = policy.stateApplicability.orEmpty().associate { item ->
            val id = requireText(item.id, "stateGate.id")
            id to PneumaticStateGate(
                id = id,
                variantGate = requireText(item.variantGate, "stateGate.variantGate"),
            )
        }

        require(rules.isNotEmpty()) { "pneumatic rule list is empty" }
        require(gates.isNotEmpty()) { "pneumatic state gates are empty" }
        return PneumaticVariantPolicy(rules, gates)
    }
}

data class AtlasVariantContext(
    val profileId: String,
    val explicitFeatures: Set<String> = emptySet(),
    val actualSectionDrawingConfirmed: Boolean = false,
    val unknownEquipmentReplacement: Boolean = false,
    val km395ExactDetailRequested: Boolean = false,
    val fullKm395IndexConfirmed: Boolean = false,
    val threeSectionOperation: Boolean = false,
    val safetyEquipmentConfigurationUncertain: Boolean = false,
    val rheostaticCapabilityConfirmed: Boolean = false,
)

data class AtlasVariantDecision(
    val exactDetailAllowed: Boolean,
    val applicableOverlayIds: Set<String> = emptySet(),
    val applicableRuleIds: Set<String> = emptySet(),
    val notices: List<String> = emptyList(),
    val blockingReasons: Set<String> = emptySet(),
)

class AtlasVariantPolicyResolver(
    private val catalog: AtlasVariantCatalog,
    private val electricalPolicy: ElectricalVariantPolicy,
    private val pneumaticPolicy: PneumaticVariantPolicy,
) {
    fun resolveElectrical(
        schemeId: String,
        context: AtlasVariantContext,
    ): AtlasVariantDecision {
        val profile = catalog.profiles[context.profileId]
            ?: return AtlasVariantDecision(
                exactDetailAllowed = false,
                blockingReasons = setOf("UNKNOWN_PROFILE"),
            )

        val effectiveFeatures = profile.features + context.explicitFeatures
        val overlays = electricalPolicy.overlays.filter { overlay ->
            schemeId in overlay.appliesTo &&
                triggerMatches(overlay, effectiveFeatures, context.explicitFeatures)
        }

        val blockers = linkedSetOf<String>()
        if (profileRequiresDrawing(profile)) {
            blockers += "PROFILE_REQUIRES_SECTION_DRAWING"
        }

        overlays.filter(::overlayRequiresDrawing).forEach { overlay ->
            blockers += "OVERLAY_REQUIRES_SECTION_DRAWING:${overlay.id}"
        }

        val notices = overlays.flatMap { overlay ->
            buildList {
                add(overlay.title)
                overlay.evidenceNote?.let(::add)
            }
        }

        return AtlasVariantDecision(
            exactDetailAllowed =
                context.actualSectionDrawingConfirmed || blockers.isEmpty(),
            applicableOverlayIds = overlays.map { it.id }.toSet(),
            notices = notices,
            blockingReasons =
                if (context.actualSectionDrawingConfirmed) emptySet() else blockers,
        )
    }

    fun resolvePneumatic(
        stateId: String?,
        context: AtlasVariantContext,
    ): AtlasVariantDecision {
        val profile = catalog.profiles[context.profileId]
            ?: return AtlasVariantDecision(
                exactDetailAllowed = false,
                blockingReasons = setOf("UNKNOWN_PROFILE"),
            )

        val blockers = linkedSetOf<String>()
        val rules = linkedSetOf<String>()
        val notices = mutableListOf<String>()

        if (profileRequiresDrawing(profile) || context.unknownEquipmentReplacement) {
            rules += "VL-PN-VAR-ACTUAL-SECTION"
            blockers += "ACTUAL_SECTION_CONFIGURATION_REQUIRED"
        }

        if (context.km395ExactDetailRequested && !context.fullKm395IndexConfirmed) {
            rules += "VL-PN-VAR-KM395-INDEX"
            blockers += "KM395_FULL_INDEX_REQUIRED"
        }

        if (context.threeSectionOperation) {
            rules += "VL-PN-VAR-THIRD-SECTION"
            notices +=
                "Трёхсекционная работа: ПМ/ТМ остаются межсекционно связанными; третью секцию нельзя автоматически считать участвующей в реостатном торможении."
        }

        if ("kt6el_high_speed_compressor" in context.explicitFeatures) {
            rules += "VL-PN-VAR-COMP-882-883"
            notices +=
                "Историческое исполнение секций 882/883: сохраняется отдельная проверка компрессорного оборудования."
        }

        if (context.safetyEquipmentConfigurationUncertain) {
            rules += "VL-PN-VAR-SAFETY-EQUIPMENT"
            blockers += "ACTUAL_SAFETY_EQUIPMENT_REQUIRED"
        }

        stateId?.let { id ->
            pneumaticPolicy.stateGates[id]?.let { gate ->
                if (
                    "SECTION_PROFILE_REQUIRED" in gate.variantGate &&
                    profile.id == "vl80s_unknown"
                ) {
                    blockers += "SECTION_PROFILE_REQUIRED:$id"
                }
                if (
                    "RHEOSTATIC_CAPABILITY_REQUIRED" in gate.variantGate &&
                    !context.rheostaticCapabilityConfirmed
                ) {
                    blockers += "RHEOSTATIC_CAPABILITY_REQUIRED:$id"
                }
            }
        }

        return AtlasVariantDecision(
            exactDetailAllowed =
                context.actualSectionDrawingConfirmed || blockers.isEmpty(),
            applicableRuleIds = rules,
            notices = notices,
            blockingReasons =
                if (context.actualSectionDrawingConfirmed) emptySet() else blockers,
        )
    }

    private fun triggerMatches(
        overlay: ElectricalVariantOverlay,
        effectiveFeatures: Set<String>,
        explicitFeatures: Set<String>,
    ): Boolean {
        val source = if (overlay.serialAutoInference) {
            effectiveFeatures
        } else {
            explicitFeatures
        }
        val enabled = overlay.triggerFeature in source
        return enabled == overlay.triggerValue
    }

    private fun profileRequiresDrawing(profile: AtlasVariantProfile): Boolean =
        profile.id == "vl80s_unknown" ||
            profile.id == "vl80s_modified_or_mixed" ||
            "REQUIRES_DRAWING" in profile.confidence ||
            profile.confidence == "PROFILE_REQUIRED"

    private fun overlayRequiresDrawing(overlay: ElectricalVariantOverlay): Boolean =
        "PRIMARY_SECTION_DRAWING_REQUIRED" in overlay.status ||
            "EVIDENCE_CONFLICT" in overlay.status
}

private data class JsonProfileCatalog(
    val physicalBuckets: List<JsonProfile>? = null,
)

private data class JsonProfile(
    val id: String? = null,
    val confidence: String? = null,
    val features: List<String>? = null,
    val rules: List<String>? = null,
)

private data class JsonElectricalDocument(
    val policy: JsonElectricalPolicy? = null,
)

private data class JsonElectricalPolicy(
    val variantOverlays: List<JsonElectricalOverlay>? = null,
)

private data class JsonElectricalOverlay(
    val id: String? = null,
    val title: String? = null,
    val trigger: JsonElectricalTrigger? = null,
    val appliesTo: List<String>? = null,
    val action: String? = null,
    val status: String? = null,
    val evidenceNote: String? = null,
)

private data class JsonElectricalTrigger(
    val feature: String? = null,
    val value: Boolean? = null,
    val serialAutoInference: Boolean? = null,
)

private data class JsonPneumaticDocument(
    val policy: JsonPneumaticPolicy? = null,
)

private data class JsonPneumaticPolicy(
    val variantContract: JsonPneumaticContract? = null,
    val stateApplicability: List<JsonPneumaticGate>? = null,
)

private data class JsonPneumaticContract(
    val rules: List<JsonPneumaticRule>? = null,
)

private data class JsonPneumaticRule(
    val id: String? = null,
    val condition: String? = null,
    val action: String? = null,
    val status: String? = null,
)

private data class JsonPneumaticGate(
    val id: String? = null,
    val variantGate: String? = null,
)

private fun requireText(value: String?, field: String): String {
    require(!value.isNullOrBlank()) { "$field must not be blank" }
    return value
}
