package ru.railbrake.calculator.shell

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ru.railbrake.calculator.designsystem.RailContentEntryScreen
import ru.railbrake.calculator.domain.CanonicalId
import ru.railbrake.calculator.domain.ModelId
import ru.railbrake.calculator.domain.RuntimeContext
import ru.railbrake.calculator.domain.VariantId
import ru.railbrake.calculator.link.LinkNavigationResult
import ru.railbrake.calculator.link.LinkResolver
import ru.railbrake.calculator.navigation.featureDestination
import ru.railbrake.calculator.runtime.ContentPackJsonLoader
import ru.railbrake.calculator.runtime.ContentRegistry

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bootstrap = runCatching { loadVl80sVerticalSlice() }
            .onFailure { Log.e("RailBrake", "VL80S vertical slice bootstrap failed", it) }

        setContent {
            MaterialTheme {
                val loaded = bootstrap.getOrNull()
                if (loaded == null) {
                    Text("Не удалось загрузить модуль ВЛ80С.")
                    return@MaterialTheme
                }

                var currentId by remember { mutableStateOf(CanonicalId("VL-EQ-HV-002")) }
                var notice by remember { mutableStateOf<String?>(null) }

                val entry = loaded.registry.find(currentId)
                if (entry == null) {
                    Text("Связанный материал временно недоступен.")
                    return@MaterialTheme
                }

                RailContentEntryScreen(
                    entry = entry,
                    destination = entry.type.featureDestination(),
                    modelTitle = "ВЛ80С",
                    notice = notice,
                    onLink = { link ->
                        when (
                            val result = loaded.linkResolver.resolve(
                                sourceId = entry.id,
                                link = link,
                                context = loaded.context,
                            )
                        ) {
                            is LinkNavigationResult.Found -> {
                                currentId = result.target.id
                                notice = null
                            }

                            LinkNavigationResult.TargetNotPublished -> {
                                notice =
                                    "Связанная диагностика пока не опубликована: сценарий остаётся кандидатом до отдельного GOLDEN REFERENCE PASS."
                            }

                            LinkNavigationResult.LayerDenied -> {
                                notice = "Этот слой информации сейчас отключён."
                            }

                            LinkNavigationResult.TargetInapplicable,
                            LinkNavigationResult.LinkNotApplicable -> {
                                notice = "Материал не применим к выбранному исполнению."
                            }

                            LinkNavigationResult.ScopeDenied -> {
                                notice = "Связь между этими модельными блоками запрещена."
                            }

                            LinkNavigationResult.SourceNotFound,
                            LinkNavigationResult.TargetNotFound,
                            LinkNavigationResult.TypeMismatch -> {
                                notice = "Связанный материал временно недоступен."
                            }
                        }
                    },
                )
            }
        }
    }

    private fun loadVl80sVerticalSlice(): Vl80sVerticalSlice {
        val loader = ContentPackJsonLoader()
        val registry = ContentRegistry()

        val paths = listOf(
            "electric/vl80s/atlas/pantograph.vertical.pack.json",
            "electric/vl80s/technical-data/pantograph.vertical.pack.json",
            "electric/vl80s/acceptance/pantograph.vertical.pack.json",
            "electric/vl80s/diagnostics/recommended/pantograph-no-rise.candidate.pack.json",
        )

        val installationIssues = paths.flatMap { path ->
            val json = assets.open(path).bufferedReader().use { it.readText() }
            registry.install(loader.parse(json))
        }

        val graphIssues = registry.validateGlobalLinks()
        check(installationIssues.isEmpty() && graphIssues.isEmpty()) {
            "VL80S vertical slice validation failed: ${installationIssues + graphIssues}"
        }

        return Vl80sVerticalSlice(
            registry = registry,
            linkResolver = LinkResolver(registry),
            context = RuntimeContext(
                workingModelId = ModelId("vl80s"),
                viewedModelId = ModelId("vl80s"),
                activeVariantId = VariantId("vl80s-general"),
            ),
        )
    }
}

private data class Vl80sVerticalSlice(
    val registry: ContentRegistry,
    val linkResolver: LinkResolver,
    val context: RuntimeContext,
)
