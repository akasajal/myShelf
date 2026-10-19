package com.myshelf.app

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI

object CoverInput {
    const val MAX_BYTES = 8 * 1024 * 1024

    fun imageLink(text: String): URI {
        val uri = try { URI(text.trim()) } catch (_: Exception) { throw IllegalArgumentException("Enter a valid HTTPS image link.") }
        require(uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() && uri.userInfo == null) {
            "Use an HTTPS link to an image."
        }
        return uri
    }

    fun read(input: InputStream, checkCancelled: () -> Unit = {}): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            checkCancelled()
            val count = input.read(buffer)
            if (count == -1) break
            require(output.size() + count <= MAX_BYTES) { "Choose an image smaller than 8 MB." }
            output.write(buffer, 0, count)
        }
        require(output.size() > 0) { "This image is empty." }
        return output.toByteArray()
    }

    fun download(link: String, checkCancelled: () -> Unit): ByteArray {
        var uri = imageLink(link)
        repeat(6) {
            checkCancelled()
            val connection = uri.toURL().openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                connection.setRequestProperty("Accept", "image/*")
                when (val status = connection.responseCode) {
                    301, 302, 303, 307, 308 -> {
                        val location = requireNotNull(connection.getHeaderField("Location")) { "This link has no image." }
                        uri = imageLink(uri.resolve(location).toString())
                    }
                    in 200..299 -> {
                        require(connection.contentLengthLong <= MAX_BYTES) { "Choose an image smaller than 8 MB." }
                        val mime = connection.contentType.orEmpty().substringBefore(';').trim()
                        require(!mime.equals("text/html", true)) { "This is a web page. Copy the image link instead." }
                        return connection.inputStream.use { stream -> read(stream, checkCancelled) }
                    }
                    else -> throw java.io.IOException("Couldn't download the image (HTTP $status).")
                }
            } finally { connection.disconnect() }
        }
        throw IllegalArgumentException("This image link redirects too many times.")
    }
}
