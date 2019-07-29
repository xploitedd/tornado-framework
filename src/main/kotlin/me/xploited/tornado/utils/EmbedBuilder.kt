package me.xploited.tornado.utils

import net.dv8tion.jda.api.entities.EmbedType
import net.dv8tion.jda.api.entities.MessageEmbed
import java.time.OffsetDateTime

class EmbedBuilder {
    private val type: EmbedType = EmbedType.RICH // only allow for rich content (for now...)
    var title: String? = null
    var description: String? = null
    var url: String? = null
    var color: EmbedColor =
        EmbedColor.SUCCESS
    var footer: Footer? = null
    var thumbnail: Image? = null
    var image: Image? = null
    var author: Author? = null
    var fields: List<Field>? = null

    private fun getThumnail(): MessageEmbed.Thumbnail? {
        if (thumbnail == null)
            return null

        return MessageEmbed.Thumbnail(thumbnail!!.url, null, 100, 100)
    }

    private fun getAuthor(): MessageEmbed.AuthorInfo? {
        if (author == null)
            return null

        return MessageEmbed.AuthorInfo(author!!.name, author!!.url, author!!.iconUrl, null)
    }

    private fun getFooter(): MessageEmbed.Footer? {
        if (footer == null)
            return null

        return MessageEmbed.Footer(footer!!.text, footer!!.iconUrl, null)
    }

    private fun getImage(): MessageEmbed.ImageInfo? {
        if (image == null)
            return null

        return MessageEmbed.ImageInfo(image!!.url, null, 100, 100)
    }

    private fun getEmbedFields(): List<MessageEmbed.Field>? {
        if (fields == null || fields!!.isEmpty())
            return null

        return fields!!.map { MessageEmbed.Field(it.name, it.value, it.inline) }
    }

    fun build(): MessageEmbed = MessageEmbed(url,
        title,
        description,
        type,
        OffsetDateTime.now(),
        color.colorId,
        getThumnail(),
        null, // don't know what this is
        getAuthor(),
        null, // not supporting this yet
        getFooter(),
        getImage(),
        getEmbedFields()
    )

    data class Footer(val text: String, val iconUrl: String? = null)
    data class Image(val url: String)
    data class Author(val name: String, val url: String? = null, val iconUrl: String? = null)
    data class Field(val name: String, val value: String, val inline: Boolean = false)
    enum class EmbedColor(val colorId: Int) {
        SUCCESS(1628491),
        WARNING(14264600),
        ERROR(14234904)
    }
}