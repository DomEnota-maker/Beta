package ru.railbrake.calculator.link

import ru.railbrake.calculator.domain.CanonicalId
import ru.railbrake.calculator.domain.ContentLink
import ru.railbrake.calculator.domain.ContentOwner
import ru.railbrake.calculator.domain.ContentTarget
import ru.railbrake.calculator.domain.LinkScope
import ru.railbrake.calculator.domain.RuntimeContext
import ru.railbrake.calculator.domain.expectedTargetType
import ru.railbrake.calculator.domain.modelIdOrNull
import ru.railbrake.calculator.navigation.NavigationTarget
import ru.railbrake.calculator.navigation.featureDestination
import ru.railbrake.calculator.runtime.ContentRegistry
import ru.railbrake.calculator.runtime.ResolveResult

sealed interface LinkNavigationResult {
    data class Found(
        val target: NavigationTarget,
        val targetContext: RuntimeContext,
    ) : LinkNavigationResult

    data object SourceNotFound : LinkNavigationResult
    data object LinkNotApplicable : LinkNavigationResult
    data object TargetNotFound : LinkNavigationResult
    data object TypeMismatch : LinkNavigationResult
    data object ScopeDenied : LinkNavigationResult
    data object TargetInapplicable : LinkNavigationResult
    data object LayerDenied : LinkNavigationResult
}

class LinkResolver(
    private val registry: ContentRegistry,
) {
    fun resolve(
        sourceId: CanonicalId,
        link: ContentLink,
        context: RuntimeContext,
    ): LinkNavigationResult {
        val source = registry.find(sourceId) ?: return LinkNavigationResult.SourceNotFound

        if (!link.applicability.matches(context)) {
            return LinkNavigationResult.LinkNotApplicable
        }

        val target = registry.find(link.targetId) ?: return LinkNavigationResult.TargetNotFound

        if (target.type != link.type.expectedTargetType()) {
            return LinkNavigationResult.TypeMismatch
        }

        if (!registry.scopeAllows(source, target, link.scope)) {
            return LinkNavigationResult.ScopeDenied
        }

        val targetContext = when (link.scope) {
            LinkScope.EXPLICIT_CROSS_MODEL -> {
                val targetModel = (target.owner as ContentOwner.Model).modelId
                context.copy(viewedModelId = targetModel)
            }
            LinkScope.SAME_OWNER,
            LinkScope.COMMON_TARGET -> context
        }

        return when (
            registry.resolve(
                ContentTarget(target.id, target.type),
                targetContext,
            )
        ) {
            is ResolveResult.Found -> LinkNavigationResult.Found(
                target = NavigationTarget(
                    id = target.id,
                    contentType = target.type,
                    destination = target.type.featureDestination(),
                    viewedModelId = target.owner.modelIdOrNull(),
                ),
                targetContext = targetContext,
            )
            ResolveResult.NotFound -> LinkNavigationResult.TargetNotFound
            ResolveResult.TypeMismatch -> LinkNavigationResult.TypeMismatch
            ResolveResult.Inapplicable -> LinkNavigationResult.TargetInapplicable
            ResolveResult.LayerDenied -> LinkNavigationResult.LayerDenied
        }
    }
}
