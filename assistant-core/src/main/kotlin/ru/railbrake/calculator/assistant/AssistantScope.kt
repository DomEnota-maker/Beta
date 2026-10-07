package ru.railbrake.calculator.assistant

import ru.railbrake.calculator.domain.ModelId

enum class AssistantQueryMode {
    WORKING_MODEL,
    EXPLICIT_MODEL,
}

sealed interface AssistantIndexSource {
    data object Common : AssistantIndexSource
    data class Model(val modelId: ModelId) : AssistantIndexSource
}

data class AssistantScope(
    val modelId: ModelId,
    val mode: AssistantQueryMode,
    val workingModelId: ModelId?,
    val includeCommon: Boolean = true,
) {
    val indexSources: Set<AssistantIndexSource>
        get() = buildSet {
            if (includeCommon) add(AssistantIndexSource.Common)
            add(AssistantIndexSource.Model(modelId))
        }
}

sealed interface AssistantScopeResult {
    data class Ready(val scope: AssistantScope) : AssistantScopeResult
    data object MissingWorkingModel : AssistantScopeResult
}

object AssistantScopeResolver {
    fun resolve(
        workingModelId: ModelId?,
        explicitModelId: ModelId? = null,
    ): AssistantScopeResult {
        if (explicitModelId != null) {
            return AssistantScopeResult.Ready(
                AssistantScope(
                    modelId = explicitModelId,
                    mode = AssistantQueryMode.EXPLICIT_MODEL,
                    workingModelId = workingModelId,
                )
            )
        }

        val working = workingModelId ?: return AssistantScopeResult.MissingWorkingModel
        return AssistantScopeResult.Ready(
            AssistantScope(
                modelId = working,
                mode = AssistantQueryMode.WORKING_MODEL,
                workingModelId = working,
            )
        )
    }
}
