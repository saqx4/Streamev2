package com.streame.tv.data.repository

import com.streame.tv.data.model.Addon
import com.streame.tv.data.model.RuntimeKind
import com.streame.tv.data.model.StreamSource

data class MovieRuntimeRequest(
    val imdbId: String,
    val title: String,
    val year: Int?
)

data class EpisodeRuntimeRequest(
    val imdbId: String,
    val season: Int,
    val episode: Int,
    val tmdbId: Int?,
    val tvdbId: Int?,
    val genreIds: List<Int>,
    val originalLanguage: String?,
    val title: String,
    val airDate: String?
)

interface AddonRuntime {
    val kind: RuntimeKind

    suspend fun resolveMovieStreams(
        addons: List<Addon>,
        request: MovieRuntimeRequest
    ): List<StreamSource>

    suspend fun resolveEpisodeStreams(
        addons: List<Addon>,
        request: EpisodeRuntimeRequest
    ): List<StreamSource>
}
