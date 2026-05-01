package com.lagradost.cloudstream3

import okhttp3.OkHttpClient

/**
 * Bridge Streame's shared OkHttpClient into the `app` global accessor that
 * CloudStream plugins use for HTTP. Called from `StreameApplication.onCreate()`
 * via `com.streame.tv.cloudstream.initCloudstream(client)`.
 *
 * Upstream CloudStream constructs `app` eagerly in `MainActivity.kt` using
 * the default `OkHttpClient`. We replace its baseClient at startup so
 * plugins hit the network via Streame's DNS-over-HTTPS, connection pool,
 * logging and interceptor stack.
 */
fun setCloudstreamHttpClient(client: OkHttpClient) {
    app.baseClient = client
}
