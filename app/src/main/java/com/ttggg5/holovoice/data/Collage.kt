package com.ttggg5.holovoice.data

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * A "picture" made of several talent pictures, encoded as a URI so it can travel wherever a
 * single artwork URI goes (database values, media metadata, the notification).
 */
object Collage {
    const val SCHEME = "holovoice-collage"
    private const val PREFIX = "$SCHEME://c?"

    /** Encodes [images] (2 or more) as a collage URI. */
    fun uri(images: List<String>): String =
        PREFIX + images.joinToString("&") { "img=" + URLEncoder.encode(it, "UTF-8") }

    /** The pictures of a collage URI ([model] may be a String or a Uri), or null for anything else. */
    fun parse(model: Any?): List<String>? {
        val text = model?.toString() ?: return null
        if (!text.startsWith(PREFIX)) return null
        return text.removePrefix(PREFIX).split('&')
            .filter { it.startsWith("img=") }
            .map { URLDecoder.decode(it.removePrefix("img="), "UTF-8") }
            .takeIf { it.isNotEmpty() }
    }
}
