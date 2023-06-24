/*
 * Copyright 2022 Viascom Ltd liab. Co
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
import io.viascom.discord.bot.aluna.util.toEditData
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import java.time.OffsetDateTime

@JsonInclude(Include.NON_NULL)
data class Webhook(
    var content: String?,
    var embeds: List<Embed>? = arrayListOf()
) {

    @JsonInclude(Include.NON_NULL)
    data class Embed(
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

        fun fromEmbedBuilder(embedBuilder: EmbedBuilder) = fromMessageEmbed(embedBuilder.build())

        fun fromMessageEmbed(messageEmbed: MessageEmbed) {
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

        fun toEmbedBuilder() = EmbedBuilder.fromData(DataObject.fromJson(ObjectMapper().writeValueAsBytes(this)))
        fun toMessageEmbed() = this.toEmbedBuilder().build()
    }

    @JsonInclude(Include.NON_NULL)
    data class Field(
        var `inline`: Boolean = true,
        var name: String,
        var value: String
    )

    @JsonInclude(Include.NON_NULL)
    data class Author(
        @JsonProperty("icon_url")
        @SerializedName("icon_url")
        var iconUrl: String?,
        var name: String?,
        var url: String?
    )

    @JsonInclude(Include.NON_NULL)
    data class Thumbnail(
        var url: String
    )

    @JsonInclude(Include.NON_NULL)
    data class Image(
        var url: String
    )

    @JsonInclude(Include.NON_NULL)
    data class Footer(
        @JsonProperty("icon_url")
        @SerializedName("icon_url")
        var iconUrl: String?,
        var text: String
    )

    fun toMessageCreateData(): MessageCreateData {
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
                it.thumbnail?.let { thumbnail -> embed.setThumbnail(thumbnail.url) }
                it.footer?.let { footer -> embed.setFooter(footer.text, footer.iconUrl) }
                it.image?.let { image -> embed.setImage(image.url) }
                it.timestamp?.let { timestamp -> embed.setTimestamp(timestamp) }
                it.fields?.ifEmpty { null }
                    ?.let { fields -> fields.forEach { field -> embed.addField(MessageEmbed.Field(field.name, field.value, field.inline)) } }
                messageEmbeds.add(embed.build())
            }
        }
        message.setEmbeds(messageEmbeds)

        return message.build()
    }

    fun toMessageEditData(): MessageEditData = toMessageCreateData().toEditData()

    companion object {
        fun fromMessage(message: MessageData): Webhook {
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
    }

}
