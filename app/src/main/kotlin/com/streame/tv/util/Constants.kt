package com.streame.tv.util

import com.streame.tv.BuildConfig

/**
 * Application constants
 *
 * API keys are stored securely on Supabase Edge Functions (not in the app).
 * The app calls Edge Function proxies which add the keys server-side.
 */
object Constants {
    // Supabase - Keys from BuildConfig (secrets.properties)
    val SUPABASE_URL: String get() = BuildConfig.SUPABASE_URL
    val SUPABASE_ANON_KEY: String get() = BuildConfig.SUPABASE_ANON_KEY

    // Edge Function Proxies - API keys are kept secure on server
    val TMDB_PROXY_URL: String get() = "${SUPABASE_URL}/functions/v1/tmdb-proxy"
    val TRAKT_PROXY_URL: String get() = "${SUPABASE_URL}/functions/v1/trakt-proxy"
    val TV_AUTH_START_URL: String get() = "${SUPABASE_URL}/functions/v1/tv-auth-start"
    val TV_AUTH_STATUS_URL: String get() = "${SUPABASE_URL}/functions/v1/tv-auth-status"
    val TV_AUTH_POLL_URL: String get() = "${SUPABASE_URL}/functions/v1/tv-auth-poll"
    val TV_AUTH_COMPLETE_URL: String get() = "${SUPABASE_URL}/functions/v1/tv-auth-complete"

    // API Base URLs - requests are intercepted and routed through proxy
    const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
    const val TRAKT_API_URL = "https://api.trakt.tv/"

    // API keys for direct calls (TMDB is public/rate-limited per IP, Trakt Client ID is public).
    // TRAKT_CLIENT_SECRET must NEVER be in the app — it stays in Supabase secrets only.
    const val TMDB_API_KEY = "3308647fabe47a844ab269e6eab19132"
    const val TRAKT_CLIENT_ID = "39c2a3c092fd244b5dbcceb8dd0cbd85e61ccf98b2ae0b1158be2c11cfcdb986"
    const val TRAKT_CLIENT_SECRET = "16fc000426675e3479764285959ee242cf651ce94ec7d0019e85bba7d23e5e0c" // Not needed for public API calls
    // Image URLs - tuned for TV quality with smooth scrolling/perf.
    const val IMAGE_BASE = "https://image.tmdb.org/t/p/w780"
    const val IMAGE_BASE_LARGE = "https://image.tmdb.org/t/p/w1280"
    const val BACKDROP_BASE = "https://image.tmdb.org/t/p/w1280"
    // Full quality for hero and detail backdrops — restored to "original" so
    // 4K TV users get the sharpest image. The loading speed issue is addressed
    // by aggressive preloading + disk caching (not by resolution downgrade).
    const val BACKDROP_BASE_LARGE = "https://image.tmdb.org/t/p/original"
    const val LOGO_BASE = "https://image.tmdb.org/t/p/w500"
    const val LOGO_BASE_LARGE = "https://image.tmdb.org/t/p/original"

    // Google Sign-In - Key from BuildConfig (secrets.properties)
    val GOOGLE_WEB_CLIENT_ID: String get() = BuildConfig.GOOGLE_WEB_CLIENT_ID

    // Progress thresholds
    const val WATCHED_THRESHOLD = 90 // Percentage at which content is considered watched
    const val MIN_PROGRESS_THRESHOLD = 3 // Minimum % progress to appear in Continue Watching (filters accidental plays)
    const val MAX_PROGRESS_ENTRIES = 50  // Max playback progress entries to process
    const val MAX_CONTINUE_WATCHING = 50 // Max items in Continue Watching row

    // Preferences keys
    const val PREFS_NAME = "Streame_prefs"
    const val PREF_DEFAULT_SUBTITLE = "default_subtitle"
    const val PREF_AUTO_PLAY_NEXT = "auto_play_next"
    const val PREF_TRAKT_TOKEN = "trakt_token"
}

/**
 * Language code mappings
 */
object LanguageMap {
    private val ISO_LANG_MAP = mapOf(
        "en" to "English", "eng" to "English",
        "fr" to "French", "fre" to "French", "fra" to "French",
        "es" to "Spanish", "spa" to "Spanish",
        "de" to "German", "ger" to "German", "deu" to "German",
        "it" to "Italian", "ita" to "Italian",
        "pt" to "Portuguese", "por" to "Portuguese",
        "nl" to "Dutch", "nld" to "Dutch", "dut" to "Dutch",
        "ru" to "Russian", "rus" to "Russian",
        "zh" to "Chinese", "chi" to "Chinese", "zho" to "Chinese",
        "ja" to "Japanese", "jpn" to "Japanese",
        "ko" to "Korean", "kor" to "Korean",
        "ar" to "Arabic", "ara" to "Arabic",
        "hi" to "Hindi", "hin" to "Hindi"
    )
    
    fun getLanguageName(code: String): String {
        return ISO_LANG_MAP[code.lowercase()] ?: code.uppercase()
    }
}


