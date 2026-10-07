package ru.railbrake.calculator.acceptance

import android.content.Context
import ru.railbrake.calculator.domain.CanonicalId

enum class AcceptanceCheckState(val label: String) {
    NOT_CHECKED("Не проверено"),
    CHECKED("Проверено"),
    NOTE("Замечание"),
    NOT_APPLICABLE("Не применяется"),
}

data class AcceptanceItemState(
    val state: AcceptanceCheckState = AcceptanceCheckState.NOT_CHECKED,
    val note: String = "",
)

data class AcceptanceSummary(
    val total: Int,
    val checked: Int,
    val notes: Int,
    val notApplicable: Int,
    val notChecked: Int,
) {
    val complete: Boolean get() = total > 0 && notChecked == 0
}

fun acceptanceSummary(states: Collection<AcceptanceItemState>): AcceptanceSummary =
    AcceptanceSummary(
        total = states.size,
        checked = states.count { it.state == AcceptanceCheckState.CHECKED },
        notes = states.count { it.state == AcceptanceCheckState.NOTE },
        notApplicable = states.count { it.state == AcceptanceCheckState.NOT_APPLICABLE },
        notChecked = states.count { it.state == AcceptanceCheckState.NOT_CHECKED },
    )

interface AcceptanceStateStore {
    fun read(id: CanonicalId): AcceptanceItemState
    fun write(id: CanonicalId, value: AcceptanceItemState)
    fun clear(id: CanonicalId)
}

class SharedPreferencesAcceptanceStateStore(
    context: Context,
) : AcceptanceStateStore {
    private val preferences =
        context.getSharedPreferences("railbrake_acceptance_state_v1", Context.MODE_PRIVATE)

    override fun read(id: CanonicalId): AcceptanceItemState {
        val state = preferences.getString(stateKey(id), AcceptanceCheckState.NOT_CHECKED.name)
            ?.let { runCatching { AcceptanceCheckState.valueOf(it) }.getOrNull() }
            ?: AcceptanceCheckState.NOT_CHECKED
        val note = preferences.getString(noteKey(id), "").orEmpty()
        return AcceptanceItemState(state = state, note = note)
    }

    override fun write(id: CanonicalId, value: AcceptanceItemState) {
        preferences.edit().apply {
            putString(stateKey(id), value.state.name)
            if (value.note.isBlank()) remove(noteKey(id))
            else putString(noteKey(id), value.note.trim())
        }.apply()
    }

    override fun clear(id: CanonicalId) {
        preferences.edit()
            .remove(stateKey(id))
            .remove(noteKey(id))
            .apply()
    }

    private fun stateKey(id: CanonicalId) = "state:${id.value}"
    private fun noteKey(id: CanonicalId) = "note:${id.value}"
}
