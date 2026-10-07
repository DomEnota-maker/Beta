package ru.railbrake.calculator.domain

enum class LocomotiveFamily {
    ELECTRIC,
    DIESEL,
}

enum class ModelSection {
    ACCEPTANCE,
    ATLAS,
    DIAGNOSTICS,
    TECHNICAL_DATA,
}

enum class CommonSection {
    FIRST_AID,
    KNOWLEDGE,
    SAFETY,
}

enum class DiagnosticCorpus {
    RECOMMENDED,
    EXTENDED,
}

enum class ExtendedDiagnosticClass {
    ARCHIVED_OFFICIAL,
    MANUFACTURER_EXTENDED,
    HISTORICAL_TRAINING,
    FIELD_PRACTICE,
}

data class DiagnosticPlacement(
    val corpus: DiagnosticCorpus,
    val extendedClass: ExtendedDiagnosticClass? = null,
) {
    init {
        require(
            (corpus == DiagnosticCorpus.RECOMMENDED && extendedClass == null) ||
                (corpus == DiagnosticCorpus.EXTENDED && extendedClass != null)
        ) {
            "extendedClass must exist only for EXTENDED diagnostic corpus"
        }
    }
}

data class ModelBlockDescriptor(
    val modelId: ModelId,
    val family: LocomotiveFamily,
    val variantIds: Set<VariantId>,
    val rootPath: String,
) {
    init {
        require(rootPath.isNotBlank()) { "rootPath must not be blank" }
    }
}
