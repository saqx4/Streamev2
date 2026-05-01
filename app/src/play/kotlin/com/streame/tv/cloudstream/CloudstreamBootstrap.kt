package com.streame.tv.cloudstream

import okhttp3.OkHttpClient

/**
 * Play-flavor no-op init. Referenced from [com.streame.tv.StreameApplication] on
 * startup; the sideload flavor's counterpart wires Streame's OkHttpClient into
 * the CloudStream runtime, but play builds have no runtime to wire, so this
 * is an empty body.
 */
@Suppress("UNUSED_PARAMETER")
fun initCloudstream(client: OkHttpClient) = Unit
