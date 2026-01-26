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

package io.viascom.discord.bot.aluna.bot.handler

import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.every
import io.mockk.mockk
import io.viascom.discord.bot.aluna.property.AlunaDiscordProperties
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.sharding.ShardManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for [DefaultFastMutualGuildsCache].
 * 
 * Tests cover:
 * - No null values in returned collections
 * - Thread safety under concurrent modifications
 * - Speed/performance characteristics
 * - Memory efficiency
 * - General expected behavior
 */
class DefaultFastMutualGuildsCacheTest {

    private lateinit var cache: DefaultFastMutualGuildsCache
    private lateinit var shardManager: ShardManager
    private lateinit var alunaProperties: AlunaProperties

    @BeforeEach
    fun setup() {
        shardManager = mockk(relaxed = true) {
            every { guilds } returns mutableListOf()
            every { getGuildById(any<Long>()) } returns null
            every { getUserById(any<Long>()) } returns null
        }

        alunaProperties = AlunaProperties().apply {
            discord = AlunaDiscordProperties().apply {
                fastMutualGuildCache = AlunaDiscordProperties.FastMutualGuildCacheProperties().apply {
                    invalidateBeforeSeedingCache = false
                    useShardManagerFallback = false
                }
            }
        }

        cache = DefaultFastMutualGuildsCache(shardManager, alunaProperties)
    }

    // ==================== NULL VALUE TESTS ====================

    @Test
    fun `get returns empty collection for unknown user`() {
        val result = cache[12345L]

        expect(result).toBeEmpty()
    }

    @Test
    fun `get never returns null values in collection`() {
        val userId = 1L
        cache.add(userId, 100L)
        cache.add(userId, 200L)
        cache.add(userId, 300L)

        val result = cache[userId]

        expect(result).toHaveSize(3)
        result.forEach { guildId ->
            expect(guildId).toBeGreaterThan(0L)
        }
    }

    @Test
    fun `get returns no null values after concurrent modifications`() {
        val userId = 1L
        val iterations = 1000
        val executor = Executors.newFixedThreadPool(8)
        val latch = CountDownLatch(iterations * 2)
        val nullFound = AtomicBoolean(false)
        val zeroFound = AtomicBoolean(false)

        // Concurrently add and read
        repeat(iterations) { i ->
            executor.submit {
                try {
                    cache.add(userId, (i + 1).toLong())
                } finally {
                    latch.countDown()
                }
            }
            executor.submit {
                try {
                    // Read while modifications are happening
                    val result = cache[userId]
                    result.forEach { guildId ->
                        if (guildId == 0L) {
                            zeroFound.set(true)
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        expect(nullFound.get()).toEqual(false)
        expect(zeroFound.get()).toEqual(false)
    }

    @Test
    fun `get returns no null values during heavy concurrent read-write`() {
        val userCount = 100
        val guildCount = 50
        val readerThreads = 4
        val writerThreads = 4
        val testDurationMs = 2000L

        val executor = Executors.newFixedThreadPool(readerThreads + writerThreads)
        val running = AtomicBoolean(true)
        val nullOrZeroFound = AtomicBoolean(false)
        val readCount = AtomicInteger(0)
        val writeCount = AtomicInteger(0)

        // Start writer threads
        repeat(writerThreads) { threadId ->
            executor.submit {
                var i = 0
                while (running.get()) {
                    val userId = (threadId * 1000 + (i % userCount)).toLong()
                    val guildId = ((i % guildCount) + 1).toLong()

                    when (i % 3) {
                        0 -> cache.add(userId, guildId)
                        1 -> cache.remove(userId, guildId)
                        else -> cache.add(userId, guildId + 1000)
                    }
                    writeCount.incrementAndGet()
                    i++
                }
            }
        }

        // Start reader threads
        repeat(readerThreads) { threadId ->
            executor.submit {
                var i = 0
                while (running.get()) {
                    val userId = (threadId * 1000 + (i % userCount)).toLong()
                    val result = cache[userId]

                    result.forEach { guildId ->
                        if (guildId == 0L) {
                            nullOrZeroFound.set(true)
                            running.set(false)
                        }
                    }
                    readCount.incrementAndGet()
                    i++
                }
            }
        }

        Thread.sleep(testDurationMs)
        running.set(false)
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        println("[DEBUG_LOG] Total reads: ${readCount.get()}, Total writes: ${writeCount.get()}")

        expect(nullOrZeroFound.get()) {
            toEqual(false)
        }
    }

    // ==================== SPEED/PERFORMANCE TESTS ====================

    @Test
    @Timeout(10, unit = TimeUnit.SECONDS)
    fun `add operation is fast`() {
        val iterations = 100_000
        val startTime = System.nanoTime()

        repeat(iterations) { i ->
            cache.add(i.toLong(), (i % 100).toLong() + 1)
        }

        val elapsedNanos = System.nanoTime() - startTime
        val opsPerSecond = iterations * 1_000_000_000L / elapsedNanos

        println("[DEBUG_LOG] Add: $iterations operations in ${elapsedNanos / 1_000_000}ms, $opsPerSecond ops/sec")

        // Should handle at least 100k operations per second
        expect(opsPerSecond).toBeGreaterThan(100_000L)
    }

    @Test
    @Timeout(10, unit = TimeUnit.SECONDS)
    fun `get operation is fast`() {
        // Setup: add some data
        repeat(1000) { userId ->
            repeat(10) { guildId ->
                cache.add(userId.toLong(), (guildId + 1).toLong())
            }
        }

        val iterations = 100_000
        val startTime = System.nanoTime()

        repeat(iterations) { i ->
            cache[i.toLong() % 1000]
        }

        val elapsedNanos = System.nanoTime() - startTime
        val opsPerSecond = iterations * 1_000_000_000L / elapsedNanos

        println("[DEBUG_LOG] Get: $iterations operations in ${elapsedNanos / 1_000_000}ms, $opsPerSecond ops/sec")

        // Should handle at least 100k operations per second
        expect(opsPerSecond).toBeGreaterThan(100_000L)
    }

    @Test
    @Timeout(10, unit = TimeUnit.SECONDS)
    fun `remove operation is fast`() {
        // Setup: add some data
        repeat(10_000) { userId ->
            repeat(5) { guildId ->
                cache.add(userId.toLong(), (guildId + 1).toLong())
            }
        }

        val iterations = 50_000
        val startTime = System.nanoTime()

        repeat(iterations) { i ->
            cache.remove(i.toLong() % 10_000, (i % 5 + 1).toLong())
        }

        val elapsedNanos = System.nanoTime() - startTime
        val opsPerSecond = iterations * 1_000_000_000L / elapsedNanos

        println("[DEBUG_LOG] Remove: $iterations operations in ${elapsedNanos / 1_000_000}ms, $opsPerSecond ops/sec")

        // Should handle at least 50k operations per second
        expect(opsPerSecond).toBeGreaterThan(50_000L)
    }

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    fun `concurrent read-write performance is acceptable`() {
        val executor = Executors.newFixedThreadPool(8)
        val iterations = 10_000
        val latch = CountDownLatch(iterations * 3)

        val startTime = System.nanoTime()

        // Mixed read/write workload
        repeat(iterations) { i ->
            executor.submit {
                try {
                    cache.add((i % 1000).toLong(), (i % 50 + 1).toLong())
                } finally {
                    latch.countDown()
                }
            }
            executor.submit {
                try {
                    cache[(i % 1000).toLong()]
                } finally {
                    latch.countDown()
                }
            }
            executor.submit {
                try {
                    cache.remove((i % 1000).toLong(), (i % 50 + 1).toLong())
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        val elapsedNanos = System.nanoTime() - startTime
        val totalOps = iterations * 3
        val opsPerSecond = totalOps * 1_000_000_000L / elapsedNanos

        println("[DEBUG_LOG] Concurrent: $totalOps operations in ${elapsedNanos / 1_000_000}ms, $opsPerSecond ops/sec")

        // Should handle at least 10k concurrent operations per second
        expect(opsPerSecond).toBeGreaterThan(10_000L)
    }

    // ==================== MEMORY TESTS ====================

    @Test
    fun `memory usage is efficient with primitive collections`() {
        val runtime = Runtime.getRuntime()

        // Force GC before measuring
        System.gc()
        Thread.sleep(100)
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()

        // Add a significant amount of data
        val userCount = 10_000
        val guildsPerUser = 5
        repeat(userCount) { userId ->
            repeat(guildsPerUser) { guildId ->
                cache.add(userId.toLong(), (guildId + 1).toLong())
            }
        }

        // Force GC after adding
        System.gc()
        Thread.sleep(100)
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()

        val memoryUsed = memoryAfter - memoryBefore
        val entriesCount = userCount * guildsPerUser
        val bytesPerEntry = if (entriesCount > 0) memoryUsed.toDouble() / entriesCount else 0.0

        println("[DEBUG_LOG] Memory used: ${memoryUsed / 1024}KB for $entriesCount entries")
        println("[DEBUG_LOG] Approximately ${bytesPerEntry.toLong()} bytes per entry")

        // With primitive long collections, we should be using much less than 
        // boxed Long objects (which would be ~24 bytes + object overhead each)
        // A reasonable expectation is under 100 bytes per entry including all overhead
        expect(bytesPerEntry).toBeLessThan(100.0)
    }

    @Test
    fun `get returns defensive copy not sharing internal state`() {
        val userId = 1L
        cache.add(userId, 100L)
        cache.add(userId, 200L)

        val result1 = cache[userId]
        val result2 = cache[userId]

        // Results should be equal but not the same object
        expect(result1.toSet()).toEqual(result2.toSet())

        // Verify that modifications to one result don't affect the other
        // (This proves we're returning a copy, not the internal structure)
        if (result1 is MutableCollection<*>) {
            @Suppress("UNCHECKED_CAST")
            (result1 as MutableCollection<Long>).clear()
        }

        val result3 = cache[userId]
        expect(result3).toHaveSize(2)
    }

    // ==================== BEHAVIORAL TESTS ====================

    @Test
    fun `add stores guild for user`() {
        cache.add(1L, 100L)

        val result = cache[1L]

        expect(result).toHaveSize(1)
        expect(result).toContain(100L)
    }

    @Test
    fun `add multiple guilds for same user`() {
        cache.add(1L, 100L)
        cache.add(1L, 200L)
        cache.add(1L, 300L)

        val result = cache[1L]

        expect(result).toHaveSize(3)
        expect(result).toContain(100L, 200L, 300L)
    }

    @Test
    fun `add same guild twice does not duplicate`() {
        cache.add(1L, 100L)
        cache.add(1L, 100L)

        val result = cache[1L]

        expect(result).toHaveSize(1)
    }

    @Test
    fun `remove removes guild from user`() {
        cache.add(1L, 100L)
        cache.add(1L, 200L)

        cache.remove(1L, 100L)

        val result = cache[1L]
        expect(result).toHaveSize(1)
        expect(result).toContain(200L)
        expect(result).notToContain(100L)
    }

    @Test
    fun `remove last guild invalidates user entry`() {
        cache.add(1L, 100L)

        cache.remove(1L, 100L)

        val result = cache[1L]
        expect(result).toBeEmpty()
        expect(cache.size).toEqual(0)
    }

    @Test
    fun `removeGuild removes guild from all users`() {
        cache.add(1L, 100L)
        cache.add(1L, 200L)
        cache.add(2L, 100L)
        cache.add(3L, 100L)
        cache.add(3L, 300L)

        cache.removeGuild(100L)

        expect(cache[1L].toList()).toContainExactly(200L)
        expect(cache[2L]).toBeEmpty()
        expect(cache[3L].toList()).toContainExactly(300L)
    }

    @Test
    fun `clear removes all entries`() {
        cache.add(1L, 100L)
        cache.add(2L, 200L)
        cache.add(3L, 300L)

        cache.clear("test")

        expect(cache.size).toEqual(0)
        expect(cache[1L]).toBeEmpty()
        expect(cache[2L]).toBeEmpty()
        expect(cache[3L]).toBeEmpty()
    }

    @Test
    fun `size reflects number of users in cache`() {
        expect(cache.size).toEqual(0)

        cache.add(1L, 100L)
        expect(cache.size).toEqual(1)

        cache.add(2L, 200L)
        expect(cache.size).toEqual(2)

        cache.add(1L, 101L) // Same user, different guild
        expect(cache.size).toEqual(2) // Size should still be 2 (users, not guilds)
    }

    // ==================== THREAD SAFETY TESTS ====================

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    fun `concurrent add operations do not corrupt state`() {
        val userId = 1L
        val threadCount = 8
        val guildsPerThread = 1000
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        repeat(threadCount) { threadId ->
            executor.submit {
                try {
                    repeat(guildsPerThread) { i ->
                        val guildId = (threadId * guildsPerThread + i + 1).toLong()
                        cache.add(userId, guildId)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        val result = cache[userId]
        val expectedSize = threadCount * guildsPerThread

        expect(result).toHaveSize(expectedSize)

        // Verify no duplicates or missing values
        val uniqueGuildIds = result.toSet()
        expect(uniqueGuildIds).toHaveSize(expectedSize)

        // Verify no null/zero values
        result.forEach { guildId ->
            expect(guildId).toBeGreaterThan(0L)
        }
    }

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    fun `concurrent add and remove operations maintain consistency`() {
        val userCount = 100
        val executor = Executors.newFixedThreadPool(8)
        val iterations = 5000
        val latch = CountDownLatch(iterations * 2)
        val errors = ConcurrentHashMap<String, AtomicInteger>()

        // Concurrent adds
        repeat(iterations) { i ->
            executor.submit {
                try {
                    val userId = (i % userCount).toLong()
                    cache.add(userId, (i % 50 + 1).toLong())
                } catch (e: Exception) {
                    errors.computeIfAbsent("add") { AtomicInteger() }.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        // Concurrent removes
        repeat(iterations) { i ->
            executor.submit {
                try {
                    val userId = (i % userCount).toLong()
                    cache.remove(userId, (i % 50 + 1).toLong())
                } catch (e: Exception) {
                    errors.computeIfAbsent("remove") { AtomicInteger() }.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        expect(errors).toBeEmpty()

        // Verify cache is in consistent state
        repeat(userCount) { userId ->
            val result = cache[userId.toLong()]
            result.forEach { guildId ->
                expect(guildId).toBeGreaterThan(0L)
            }
        }
    }

    @Test
    @Timeout(30, unit = TimeUnit.SECONDS)
    fun `concurrent removeGuild does not cause issues`() {
        // Setup: add data for multiple users across multiple guilds
        val userCount = 100
        val guildCount = 20
        repeat(userCount) { userId ->
            repeat(guildCount) { guildId ->
                cache.add(userId.toLong(), (guildId + 1).toLong())
            }
        }

        val executor = Executors.newFixedThreadPool(4)
        val latch = CountDownLatch(guildCount)
        val errors = AtomicInteger(0)

        // Remove all guilds concurrently
        repeat(guildCount) { guildId ->
            executor.submit {
                try {
                    cache.removeGuild((guildId + 1).toLong())
                } catch (e: Exception) {
                    errors.incrementAndGet()
                    e.printStackTrace()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        expect(errors.get()).toEqual(0)
        expect(cache.size).toEqual(0)
    }
}
