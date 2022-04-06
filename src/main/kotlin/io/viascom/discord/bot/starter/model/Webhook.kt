package io.viascom.discord.bot.starter.model

import com.google.gson.annotations.SerializedName
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import java.time.OffsetDateTime

data class Webhook(
    var content: String?,
    var embeds: List<Embed>? = arrayListOf()
) {

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
    )

    data class Field(
        var `inline`: Boolean = true,
        var name: String,
        var value: String
    )

    data class Author(
        @SerializedName("icon_url")
        var iconUrl: String?,
        var name: String?,
        var url: String?
    )

    data class Thumbnail(
        var url: String
    )

    data class Image(
        var url: String
    )

    data class Footer(
        @SerializedName("icon_url")
        var iconUrl: String?,
        var text: String
    )

    fun toMessage(): Message {
        val message = MessageBuilder()
        content?.let { message.setContent(it) }

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
                it.fields?.let { fields -> fields.forEach { field -> embed.addField(MessageEmbed.Field(field.name, field.value, field.inline)) } }
                messageEmbeds.add(embed.build())
            }
        }

        return message.build()
    }

    companion object {
        fun fromMessage(message: Message): Webhook {
            val embeds = message.embeds.map { embed ->
                val author = embed.author?.let { Author(it.iconUrl, it.name, it.url) }
                val fields = embed.fields.map { Field(it.isInline, it.name ?: "", it.value ?: "") }
                val footer = embed.footer?.let { Footer(it.iconUrl, it.text ?: "") }
                val image = embed.image?.url?.let { Image(it) }
                val thumbnail = embed.thumbnail?.url?.let { Thumbnail(it) }

                Embed(author, embed.colorRaw, embed.description, fields, footer, image, thumbnail, embed.timestamp, embed.title, embed.url)
            }

            return Webhook(message.contentRaw, embeds)
        }
    }

}
