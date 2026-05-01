package com.streame.tv.data.repository

import com.streame.tv.data.model.Addon
import com.streame.tv.data.model.RuntimeKind
import com.streame.tv.data.model.StreamSource

class AddonRuntimeAggregator(
    private val addonRuntimes: Map<RuntimeKind, AddonRuntime>
) {
    suspend fun resolveMovieStreams(
        stremioAddons: List<Addon>,
        cloudstreamAddons: List<Addon>,
        request: MovieRuntimeRequest
    ): List<StreamSource> {
        val streams = mutableListOf<StreamSource>()
        if (stremioAddons.isNotEmpty()) {
            streams += addonRuntimes[RuntimeKind.STREMIO]
                ?.resolveMovieStreams(stremioAddons, request)
                .orEmpty()
        }
        if (cloudstreamAddons.isNotEmpty()) {
            streams += addonRuntimes[RuntimeKind.CLOUDSTREAM]
                ?.resolveMovieStreams(cloudstreamAddons, request)
                .orEmpty()
        }
        return streams
    }

    suspend fun resolveEpisodeStreams(
        stremioAddons: List<Addon>,
        cloudstreamAddons: List<Addon>,
        request: EpisodeRuntimeRequest
    ): List<StreamSource> {
        val streams = mutableListOf<StreamSource>()
        if (stremioAddons.isNotEmpty()) {
            streams += addonRuntimes[RuntimeKind.STREMIO]
                ?.resolveEpisodeStreams(stremioAddons, request)
                .orEmpty()
        }
        if (cloudstreamAddons.isNotEmpty()) {
            streams += addonRuntimes[RuntimeKind.CLOUDSTREAM]
                ?.resolveEpisodeStreams(cloudstreamAddons, request)
                .orEmpty()
        }
        return streams
    }
}
