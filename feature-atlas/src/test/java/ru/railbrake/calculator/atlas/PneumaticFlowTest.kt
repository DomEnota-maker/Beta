package ru.railbrake.calculator.atlas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PneumaticFlowTest {
    private val mode = PneumaticFlowMode(
        id = "SERVICE_BRAKE",
        title = "Служебное",
        summary = "Summary",
        start = "Start",
        note = null,
        steps = listOf(
            PneumaticFlowStep(
                title = "1",
                description = "one",
                segments = listOf(
                    PneumaticRouteSegment(
                        PneumaticRouteKind.FLOW,
                        listOf(
                            PneumaticRoutePoint(0f, 0f),
                            PneumaticRoutePoint(10f, 10f),
                        ),
                    )
                ),
            ),
            PneumaticFlowStep(
                title = "2",
                description = "two",
                segments = listOf(
                    PneumaticRouteSegment(
                        PneumaticRouteKind.CONTROL,
                        listOf(
                            PneumaticRoutePoint(10f, 10f),
                            PneumaticRoutePoint(20f, 20f),
                        ),
                    )
                ),
            ),
        ),
    )

    @Test
    fun sessionMovesWithoutLeavingStepBounds() {
        val start = PneumaticFlowSession(mode)
        assertFalse(start.hasPrevious)
        assertTrue(start.hasNext)
        assertEquals("1", start.currentStep.title)

        val second = start.next()
        assertTrue(second.hasPrevious)
        assertFalse(second.hasNext)
        assertEquals("2", second.currentStep.title)

        assertSame(second, second.next())
        assertEquals(0, second.reset().stepIndex)
    }

    @Test(expected = IllegalArgumentException::class)
    fun documentRejectsPointOutsideDeclaredCanvas() {
        PneumaticFlowDocument(
            id = "test",
            modelId = "vl80s",
            title = "Test",
            canvasWidth = 10f,
            canvasHeight = 10f,
            coordinateClaim = "PRESENTATION",
            actionAuthority = "NONE",
            disclaimer = "Training",
            modes = listOf(
                mode.copy(
                    steps = listOf(
                        mode.steps.first().copy(
                            segments = listOf(
                                PneumaticRouteSegment(
                                    PneumaticRouteKind.FLOW,
                                    listOf(
                                        PneumaticRoutePoint(0f, 0f),
                                        PneumaticRoutePoint(11f, 10f),
                                    ),
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun backgroundCannotClaimUnalignedCoordinateSpace() {
        PneumaticFlowDocument(
            id = "test",
            modelId = "vl80s",
            title = "Test",
            canvasWidth = 30f,
            canvasHeight = 30f,
            coordinateClaim = "TRAINING",
            actionAuthority = "NONE",
            disclaimer = "Training",
            modes = listOf(mode),
            background = PneumaticFlowBackground(
                assetPath = "electric/vl80s/atlas/interactive/underlay.jpg",
                sha256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                width = 29,
                height = 30,
                sourceRepository = "verified/repository",
                sourceCommit = "source-commit",
                sourceArchive = "archive.zip",
                sourceArchiveBlobSha = "blob",
                sourceEntry = "source/underlay.jpg",
            ),
        )
    }

    @Test
    fun loaderAcceptsProvenanceOnlyWhenBackgroundAligned() {
        val json = """
            {
              "id":"test.flow",
              "modelId":"vl80s",
              "title":"Test",
              "modeCount":1,
              "semantics":{
                "actionAuthority":"NONE",
                "coordinateClaim":"TRAINING_PRESENTATION",
                "disclaimer":"No operational authority",
                "coordinateSpace":{"width":10,"height":10},
                "background":{
                  "assetPath":"electric/vl80s/atlas/interactive/underlay.jpg",
                  "sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "width":10,
                  "height":10,
                  "source":{
                    "repository":"verified/repository",
                    "commit":"source-commit",
                    "archive":"archive.zip",
                    "archiveBlobSha":"blob",
                    "entry":"source/underlay.jpg"
                  }
                }
              },
              "modes":[
                {
                  "id":"TEST",
                  "title":"test",
                  "summary":"summary",
                  "start":"start",
                  "steps":[
                    {
                      "title":"step",
                      "description":"description",
                      "segments":[
                        {
                          "kind":"FLOW",
                          "points":[{"x":0,"y":0},{"x":10,"y":10}]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val parsed = PneumaticFlowJsonLoader().parse(json)
        assertEquals("source/underlay.jpg", parsed.background?.sourceEntry)
        assertEquals(10, parsed.background?.width)
        assertEquals(1, parsed.modes.single().steps.size)
    }
}
