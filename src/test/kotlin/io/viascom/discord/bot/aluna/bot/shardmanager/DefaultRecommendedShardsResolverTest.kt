/*
 * Copyright 2025 Viascom Ltd liab. Co
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.viascom.discord.bot.aluna.bot.shardmanager

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.viascom.discord.bot.aluna.model.GatewayResponse
import io.viascom.discord.bot.aluna.property.AlunaDiscordProperties
import io.viascom.discord.bot.aluna.property.AlunaProperties
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class DefaultRecommendedShardsResolverTest {

    private val gatewayUrl = "https://gateway-proxy.test/api/gateway/bot"

    private fun properties(token: String? = "test-token", url: String = gatewayUrl) =
        AlunaProperties().apply {
            discord = AlunaDiscordProperties().apply {
                this.token = token
                this.gatewayBotUrl = url
            }
        }

    private fun response(request: Request, code: Int, bodyJson: String = "{}") =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 200..299) "OK" else "Error")
            .body(bodyJson.toResponseBody("application/json".toMediaType()))
            .build()

    @Test
    fun `resolve queries the configured url with bot auth and maps the response`() {
        val client = mockk<OkHttpClient>()
        val call = mockk<Call>()
        val objectMapper = mockk<ObjectMapper>()
        val requestSlot = slot<Request>()

        val sessionStartLimit = GatewayResponse.SessionStartLimit(
            total = 1000,
            remaining = 999,
            resetAfter = 3_600_500,
            maxConcurrency = 1,
        )
        val gatewayResponse = GatewayResponse(shards = 4, sessionStartLimit = sessionStartLimit)

        every { client.newCall(capture(requestSlot)) } returns call
        every { call.execute() } answers { response(requestSlot.captured, 200, "{\"shards\":4}") }
        every { objectMapper.readValue(any<String>(), GatewayResponse::class.java) } returns gatewayResponse

        val resolver = DefaultRecommendedShardsResolver(properties(), objectMapper, client)

        val before = LocalDateTime.now(ZoneOffset.UTC)
        val result = resolver.resolve()
        val after = LocalDateTime.now(ZoneOffset.UTC)

        expect(result != null).toEqual(true)
        val resolved = result!!
        expect(resolved.shards).toEqual(4)
        val limit = resolved.sessionStartLimit!!
        expect(limit.maxConcurrency).toEqual(1)
        // reset_after is in milliseconds: the 500 ms remainder must survive (flooring to whole
        // seconds via resetAfter / 1000 would push resetTimestamp before before + 3_600_500 ms).
        val resetTimestamp = limit.resetTimestamp!!
        expect(resetTimestamp >= before.plus(3_600_500L, ChronoUnit.MILLIS)).toEqual(true)
        expect(resetTimestamp <= after.plus(3_600_500L, ChronoUnit.MILLIS)).toEqual(true)
        expect(requestSlot.captured.url.toString()).toEqual(gatewayUrl)
        expect(requestSlot.captured.header("Authorization")).toEqual("Bot test-token")
    }

    @Test
    fun `resolve returns null on a non-2xx response and never parses`() {
        val client = mockk<OkHttpClient>()
        val call = mockk<Call>()
        val objectMapper = mockk<ObjectMapper>()
        val requestSlot = slot<Request>()

        every { client.newCall(capture(requestSlot)) } returns call
        every { call.execute() } answers { response(requestSlot.captured, 500) }

        val resolver = DefaultRecommendedShardsResolver(properties(), objectMapper, client)

        expect(resolver.resolve() == null).toEqual(true)
        verify(exactly = 0) { objectMapper.readValue(any<String>(), GatewayResponse::class.java) }
    }

    @Test
    fun `resolve returns null when the http call throws`() {
        val client = mockk<OkHttpClient>()
        val call = mockk<Call>()
        val objectMapper = mockk<ObjectMapper>()

        every { client.newCall(any()) } returns call
        every { call.execute() } throws IOException("boom")

        val resolver = DefaultRecommendedShardsResolver(properties(), objectMapper, client)

        expect(resolver.resolve() == null).toEqual(true)
    }

    @Test
    fun `resolve returns null and does not call http when the token is missing`() {
        val client = mockk<OkHttpClient>()
        val objectMapper = mockk<ObjectMapper>()

        val resolver = DefaultRecommendedShardsResolver(properties(token = null), objectMapper, client)

        expect(resolver.resolve() == null).toEqual(true)
        verify(exactly = 0) { client.newCall(any()) }
    }
}
