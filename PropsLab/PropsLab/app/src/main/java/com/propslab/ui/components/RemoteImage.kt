package com.propslab.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.propslab.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * A public ESPN headshot or club logo, with an initials tile while it loads or when the URL
 * is missing. No image library and no disk cache: the bytes are small and the user can clear
 * them by leaving the app. Nothing is uploaded.
 */
private val imageCache = object : LruCache<String, ImageBitmap>(6 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
}

@Composable
fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable BoxScope.() -> Unit,
) {
    val image by produceState<ImageBitmap?>(
        initialValue = url.takeIf { it.isNotBlank() }?.let { imageCache.get(it) },
        key1 = url,
    ) {
        if (url.isBlank()) {
            value = null
            return@produceState
        }
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Cheat Code/1.1 (Android)")
                    setRequestProperty("Accept", "image/*")
                }
                try {
                    if (connection.responseCode !in 200..299) return@runCatching null
                    connection.inputStream.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()?.also { imageCache.put(url, it) }
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(Palette.Raised),
                contentAlignment = Alignment.Center,
                content = fallback,
            )
        }
    }
}

/** The initials fallback used by both the player and club treatments. */
@Composable
fun InitialsTile(text: String, modifier: Modifier = Modifier) {
    Box(modifier.background(Palette.Raised), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = Palette.Dust,
        )
    }
}
