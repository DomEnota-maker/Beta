package ru.railbrake.calculator.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticFeatureIndexTest {
    private val pinned = """
        {
          "schemaVersion": 1,
          "modelId": "vl80s",
          "recommendedPacks": ["electric/vl80s/diagnostics/recommended/test.pack.json"],
          "extendedCorpora": {},
          "runtimePayloadStatus": "EXECUTABLE_FLOW_AVAILABLE",
          "publicationPolicy": "ALL_CANDIDATE_UNTIL_FEATURE_RUNTIME_ACCEPTANCE",
          "executableFlow": "electric/vl80s/diagnostics/runtime/executable-flow.json"
        }
    """.trimIndent()

    @Test
    fun indexKeepsRuntimeSeparateFromPublication() {
        val parsed = DiagnosticFeatureIndexJsonLoader().parse(pinned)
        assertEquals("vl80s", parsed.modelId)
        assertEquals(
            "ALL_CANDIDATE_UNTIL_FEATURE_RUNTIME_ACCEPTANCE",
            parsed.publicationPolicy,
        )
        assertEquals(
            "electric/vl80s/diagnostics/runtime/executable-flow.json",
            parsed.executableFlow,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingPublicationPolicyFailsClosed() {
        DiagnosticFeatureIndexJsonLoader().parse(
            pinned.replace(
                "\"publicationPolicy\": \"ALL_CANDIDATE_UNTIL_FEATURE_RUNTIME_ACCEPTANCE\"",
                "\"publicationPolicy\": \"\"",
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun relativeTraversalAssetIsRejected() {
        DiagnosticFeatureIndexJsonLoader().parse(
            pinned.replace(
                "electric/vl80s/diagnostics/runtime/executable-flow.json",
                "../../untrusted.json",
            )
        )
    }
}
