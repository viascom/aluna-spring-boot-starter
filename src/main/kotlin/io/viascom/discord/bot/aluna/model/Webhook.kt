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

package io.viascom.discord.bot.aluna.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.annotations.SerializedName
import io.viascom.discord.bot.aluna.util.getGuildMessage
import io.viascom.discord.bot.aluna.util.getMessage
import io.viascom.discord.bot.aluna.util.toEditData
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import java.time.OffsetDateTime

@JsonInclude(Include.NON_NULL)
public data class Webhook(
    var content: String?,
    var embeds: List<Embed>? = arrayListOf()
) {

    @JsonInclude(Include.NON_NULL)
    public data class Embed(
        var author: Author?,
        var color: Int?,
        var description: String?,
        var fields: List<Field>? = arrayListOf(),
        var footer: Footer?,
        var image: Image?,
        var thumbnail: Thumbnail?,
        var timestamp: OffsetDateTime?,
        var title: String?,
        var url: String?
    ) {

        public fun fromEmbedBuilder(embedBuilder: EmbedBuilder): Unit = fromMessageEmbed(embedBuilder.build())

        public fun fromMessageEmbed(messageEmbed: MessageEmbed) {
            messageEmbed.toData().toJson().let { json ->
                ObjectMapper().readValue(json, Embed::class.java).let { embed ->
                    this.author = embed.author
                    this.color = embed.color
                    this.description = embed.description
                    this.fields = embed.fields
                    this.footer = embed.footer
                    this.image = embed.image
                    this.thumbnail = embed.thumbnail
                    this.timestamp = embed.timestamp
                    this.title = embed.title
                    this.url = embed.url
                }
            }
        }

        public fun toEmbedBuilder(): EmbedBuilder = EmbedBuilder.fromData(DataObject.fromJson(ObjectMapper().writeValueAsBytes(this)))
        public fun toMessageEmbed(): MessageEmbed = this.toEmbedBuilder().build()

        public fun getSize(): Int {
            return (title?.length ?: 0) + (description?.length ?: 0) +
                    (fields?.sumOf { it.name.length + it.value.length } ?: 0) +
                    (author?.name?.length ?: 0) + (author?.iconUrl?.length ?: 0) +
                    (footer?.text?.length ?: 0) + (footer?.iconUrl?.length ?: 0) +
                    (thumbnail?.url?.length ?: 0) + (image?.url?.length ?: 0)
        }
    }

    @JsonInclude(Include.NON_NULL)
    public data class Field(
        var `inline`: Boolean = true,
        var name: String,
        var value: String
    )

    @JsonInclude(Include.NON_NULL)
    public data class Author(
        @param:JsonProperty("icon_url")
        @SerializedName("icon_url")
        var iconUrl: String?,
        var name: String?,
        var url: String?
    )

    @JsonInclude(Include.NON_NULL)
    public data class Thumbnail(
        var url: String? = null
    )

    @JsonInclude(Include.NON_NULL)
    public data class Image(
        var url: String? = null
    )

    @JsonInclude(Include.NON_NULL)
    public data class Footer(
        @param:JsonProperty("icon_url")
        @SerializedName("icon_url")
        var iconUrl: String?,
        var text: String
    )

    public fun toMessageCreateData(): MessageCreateData {
        val message = MessageCreateBuilder()
        content?.let { message.setContent(it) } ?: message.setContent("")

        val messageEmbeds = arrayListOf<MessageEmbed>()
        embeds?.let { embeds ->
            embeds.forEach {
                val embed = EmbedBuilder()
                it.author?.let { author -> embed.setAuthor(author.name, author.url, author.iconUrl) }
                it.title?.let { title -> embed.setTitle(title, it.url) }
                it.color?.let { color -> embed.setColor(color) }
                it.description?.let { description -> embed.setDescription(description) }
                it.thumbnail?.url?.let { thumbnail -> embed.setThumbnail(thumbnail) }
                it.footer?.let { footer -> embed.setFooter(footer.text, footer.iconUrl) }
                it.image?.url?.let { image -> embed.setImage(image) }
                it.timestamp?.let { timestamp -> embed.setTimestamp(timestamp) }
                it.fields?.ifEmpty { null }
                    ?.let { fields -> fields.forEach { field -> embed.addField(MessageEmbed.Field(field.name, field.value, field.inline)) } }
                messageEmbeds.add(embed.build())
            }
        }
        message.setEmbeds(messageEmbeds)

        return message.build()
    }

    public fun toMessageEditData(): MessageEditData = toMessageCreateData().toEditData()

    public fun getSize(): Int {
        return (content?.length ?: 0) + (embeds?.sumOf(Embed::getSize) ?: 0)
    }

    public companion object {
        public fun fromMessage(message: MessageData): Webhook {
            val embeds = message.embeds.map { embed ->
                val author = embed.author?.let { Author(it.iconUrl, it.name, it.url) }
                val fields = embed.fields.ifEmpty { null }?.map { Field(it.isInline, it.name ?: "", it.value ?: "") }
                val footer = embed.footer?.let { Footer(it.iconUrl, it.text ?: "") }
                val image = embed.image?.url?.let { Image(it) }
                val thumbnail = embed.thumbnail?.url?.let { Thumbnail(it) }

                Embed(author, embed.colorRaw, embed.description, fields, footer, image, thumbnail, embed.timestamp, embed.title, embed.url)
            }

            return Webhook(message.content.ifEmpty { null }, embeds)
        }

        public fun fromMessageLink(messageLink: String, user: User, shardManager: ShardManager): Webhook? {
            val elements = messageLink.split("/")
            val serverId = elements[4]
            val channelId = elements[5]
            val messageId = elements[6]

            val message = if (serverId == "@me") {
                try {
                    user.getMessage(messageId)
                } catch (e: Exception) {
                    null
                }
            } else {
                try {
                    shardManager.getGuildMessage(serverId, channelId, messageId)
                } catch (e: Exception) {
                    null
                }
            }

            if (message == null) {
                return null
            }

            return fromMessage(MessageEditData.fromMessage(message))
        }
    }

}
