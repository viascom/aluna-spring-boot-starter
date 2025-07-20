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

package io.viascom.discord.bot.aluna.bot.emoji

import com.fasterxml.jackson.databind.ObjectMapper
import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnEmojiManagementEnabled
import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import io.viascom.discord.bot.aluna.event.DiscordFirstShardConnectedEvent
import io.viascom.discord.bot.aluna.model.ApplicationEmoji
import io.viascom.discord.bot.aluna.model.ApplicationEmojiData
import io.viascom.discord.bot.aluna.model.ApplicationEmojiDataList
import io.viascom.discord.bot.aluna.property.AlunaProperties
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.sharding.ShardManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnJdaEnabled
@ConditionalOnEmojiManagementEnabled
public open class EmojiManager(
    private val alunaProperties: AlunaProperties,
    private val shardManager: ShardManager,
    private val emojiCache: EmojiCache
) : ApplicationListener<DiscordFirstShardConnectedEvent> {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private var latch: CountDownLatch = CountDownLatch(0)

    /**
     * Handles the initialization and synchronization of application emojis upon the connection of the first Discord shard.
     *
     * This method is triggered by the `DiscordFirstShardConnectedEvent`. It performs the following actions:
     *
     * - Checks if emoji management is enabled in the application properties. If not, logs a message and exits.
     * - Retrieves and caches the application emojis from the Discord API.
     * - Logs the number of emojis retrieved.
     * - If specific emoji synchronization flags (`uploadMissingEmojis`, `deleteMissingEmojis`, `updateOnChange`) are enabled in the application properties,
     *   it proceeds to synchronize emojis between the local cache and Discord.
     * - Marks the emojis as fully loaded in the static cache after synchronization.
     * - Logs the completion of the emoji loading process.
     */
    public fun synchronize() {
        onApplicationEvent(DiscordFirstShardConnectedEvent(this, ReadyEvent(shardManager.shards.first()), shardManager))
    }

    override fun onApplicationEvent(event: DiscordFirstShardConnectedEvent) {
        AlunaDispatchers.InternalScope.launch {
            if (!alunaProperties.emoji.enabled) {
                logger.debug("Application emoji management is disabled")
                return@launch
            }

            logger.info("Initializing application emojis")
            ApplicationEmoji.globalCache = emojiCache

            val emojis = getApplicationEmojis()
            logger.info("Found ${emojis.size} application emojis")
            emojiCache.getEmojiCache().putAll(emojis.associateBy { it.name })

            if (alunaProperties.emoji.uploadMissingEmojis || alunaProperties.emoji.deleteMissingEmojis || alunaProperties.emoji.updateOnChange) {
                synchronizeEmojis()
            }

            StaticEmojiCache.emojisLoaded = true
            logger.info("All emojis are loaded!")
        }
    }

    /**
     * Get all application emojis from Discord
     *
     * @return List of application emojis
     */
    private suspend fun getApplicationEmojis(): List<ApplicationEmojiData> {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        val client = clientBuilder.build()

        val request = Request.Builder()
            .url("https://discord.com/api/applications/${alunaProperties.discord.applicationId}/emojis")
            .addHeader("Authorization", "Bot ${alunaProperties.discord.token}")
            .addHeader("Content-Type", "application/json")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IllegalStateException("Empty response body")

        val objectMapper = ObjectMapper()
        val emojiDataList = objectMapper.readValue(responseBody, ApplicationEmojiDataList::class.java)

        return emojiDataList.items
    }

    /**
     * Synchronize emojis between the local cache and Discord
     */
    private suspend fun synchronizeEmojis() = withContext(AlunaDispatchers.Internal) {
        val storageEmojis = emojiCache.getEmojiMetaDataFromStorage()

        val missingOnDiscord = if (alunaProperties.emoji.uploadMissingEmojis) {
            storageEmojis.filter { it.name !in emojiCache.getEmojiCache().keys }.map { it.name }
        } else arrayListOf()

        val missingLocally = if (alunaProperties.emoji.deleteMissingEmojis) {
            emojiCache.getEmojiCache().filter { it.key !in storageEmojis.map { it.name } }.map { it.value.id }
        } else arrayListOf()

        val changed = if (alunaProperties.emoji.updateOnChange) {
            emojiCache.getEmojiCache().filter { emoji ->
                val dbEmoji = storageEmojis.firstOrNull { it.name == emoji.key } ?: return@filter false
                dbEmoji.modifyDate.isAfter(emoji.value.getTimeCreated().toLocalDateTime())
            }
        } else mapOf()

        latch = CountDownLatch(missingOnDiscord.size + missingLocally.size + (if (alunaProperties.emoji.updateOnChange) changed.size else 0))

        val jda = shardManager.shards.first()

        missingOnDiscord.forEach {
            val emoji = emojiCache.findEmojiFromStorageById(it) ?: return@forEach
            jda.createApplicationEmoji(emoji.name, Icon.from(emoji.image)).queue({
                emojiCache.getEmojiCache()[emoji.name] = ApplicationEmojiData.fromApplicationEmoji(it)
                latch.countDown()
            }, {
                logger.error("Error while creating emoji: ${emoji.name}", it)
                latch.countDown()
            })
        }

        missingLocally.forEach { emojiId ->
            jda.retrieveApplicationEmojiById(emojiId).queue { emoji ->
                emoji.delete().queue {
                    emojiCache.getEmojiCache().remove(emoji.name)
                    latch.countDown()
                }
            }
        }

        changed.forEach {
            val emoji = emojiCache.findEmojiFromStorageById(it.key) ?: return@forEach
            jda.retrieveApplicationEmojiById(it.value.id).queue {
                it.delete().queue {
                    jda.createApplicationEmoji(emoji.name, Icon.from(emoji.image)).queue({
                        emojiCache.getEmojiCache()[emoji.name] = ApplicationEmojiData.fromApplicationEmoji(it)
                        latch.countDown()
                    }, {
                        logger.error("Error while updating emoji: ${emoji.name}", it)
                        latch.countDown()
                    })
                }
            }
        }

        if (latch.count > 0) {
            if (missingOnDiscord.isNotEmpty()) {
                logger.info("Uploaded ${missingOnDiscord.size} emojis to discord")
            }
            if (missingLocally.isNotEmpty()) {
                logger.info("Deleted ${missingLocally.size} emojis from discord")
            }
            if (changed.isNotEmpty()) {
                logger.info("Updated ${changed.size} emojis on discord")
            }

            latch.await()
        }
    }
}
