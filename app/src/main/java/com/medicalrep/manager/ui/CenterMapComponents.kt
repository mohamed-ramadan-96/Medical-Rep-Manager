package com.medicalrep.manager.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shared Google Maps payload delivered by Android's share sheet.
 * MainActivity uses singleTask so this payload reaches the existing picker screen.
 */
object MapShareState {
    var sharedText by mutableStateOf<String?>(null)
}

fun handleMapShareIntent(intent: Intent?) {
    if (intent?.action == Intent.ACTION_SEND) {
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            ?: intent.getStringExtra(Intent.EXTRA_HTML_TEXT)
        if (!text.isNullOrBlank()) MapShareState.sharedText = text
    }
}

fun openGoogleMapsLocationPicker(context: Context, latitude: Double?, longitude: Double?) {
    val uri = if (latitude != null && longitude != null) {
        Uri.parse(String.format(Locale.US, "geo:%f,%f?q=%f,%f", latitude, longitude, latitude, longitude))
    } else {
        Uri.parse("geo:26.8,30.8?z=6")
    }
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        context.startActivity(intent)
    } catch (_: android.content.ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps")))
    }
}

fun openCenterDirections(context: Context, latitude: Double, longitude: Double) {
    val uri = Uri.parse(
        String.format(Locale.US, "https://www.google.com/maps/dir/?api=1&destination=%f,%f", latitude, longitude)
    )
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}

@Composable
fun CenterMapPreview(
    latitude: Double?,
    longitude: Double?,
    modifier: Modifier = Modifier,
    onLocationPicked: ((Double, Double) -> Unit)? = null
) {
    if (latitude != null && longitude != null) {
        androidx.compose.material3.Surface(
            modifier = modifier,
            color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("موقع محفوظ على Google Maps", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                Text(
                    String.format(Locale.US, "%.6f, %.6f", latitude, longitude),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun CenterMapPickerDialog(
    isArabic: Boolean,
    initialLatitude: Double?,
    initialLongitude: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    var latitude by remember(initialLatitude, initialLongitude) { mutableStateOf(initialLatitude) }
    var longitude by remember(initialLatitude, initialLongitude) { mutableStateOf(initialLongitude) }
    var sharedLink by remember { mutableStateOf("") }
    var linkError by remember { mutableStateOf(false) }
    var resolving by remember { mutableStateOf(false) }

    LaunchedEffect(MapShareState.sharedText) {
        val incoming = MapShareState.sharedText
        if (!incoming.isNullOrBlank()) {
            sharedLink = incoming.trim()
            linkError = false
            MapShareState.sharedText = null
            resolving = true
        }
    }

    LaunchedEffect(resolving, sharedLink) {
        if (resolving && sharedLink.isNotBlank()) {
            val resolved = resolveSharedMapLink(sharedLink)
            val parsed = parseMapCoordinates(sharedLink)
                ?: resolved?.let { parseMapCoordinates(it.url) ?: parseMapCoordinates(it.page) }
                ?: geocodeSharedMapPlace(sharedLink, resolved?.url, resolved?.page)
            if (parsed != null) {
                latitude = parsed.first
                longitude = parsed.second
                linkError = false
            } else {
                linkError = true
            }
            resolving = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "تحديد موقع المركز" else "Set center location") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (isArabic) "افتح Google Maps، وابحث عن المكان أو اختره من الخريطة، ثم افتح بطاقة المكان واضغط مشاركة واختر Medical Rep Manager. سنحاول استخراج الإحداثيات من الرابط أو تحديد المكان من اسمه تلقائيًا. ويمكنك استخدام الضغط المطوّل لوضع Pin إذا أردت تحديد نقطة دقيقة بنفسك."
                    else "Open Google Maps, search for or select the place, open its place card, tap Share, then choose Medical Rep Manager. We'll try to read coordinates from the link or locate the place by name automatically. You can still long-press to drop a pin when you need an exact custom point."
                )
                OutlinedButton(
                    onClick = { openGoogleMapsLocationPicker(context, latitude, longitude) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isArabic) "فتح Google Maps لاختيار الموقع" else "Open Google Maps to choose location")
                }
                OutlinedTextField(
                    value = sharedLink,
                    onValueChange = {
                        sharedLink = it
                        linkError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = linkError,
                    label = { Text(if (isArabic) "رابط Google Maps" else "Google Maps link") }
                )
                OutlinedButton(
                    enabled = sharedLink.isNotBlank() && !resolving,
                    onClick = {
                        resolving = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            resolving -> if (isArabic) "جارٍ استخراج الموقع..." else "Reading location..."
                            else -> if (isArabic) "استخراج الموقع من الرابط" else "Extract location from link"
                        }
                    )
                }
                if (linkError) {
                    Text(
                        if (isArabic) "تعذر تحديد المكان من الرابط أو اسمه. جرّب مشاركة المكان من بطاقة Google Maps مرة أخرى، أو استخدم الضغط المطوّل لوضع Pin."
                        else "Could not locate this place from its link or name. Try sharing it again from the Google Maps place card, or long-press to drop a pin."
                    )
                }
                if (latitude != null && longitude != null) {
                    Text(String.format(Locale.US, "Latitude: %.6f   Longitude: %.6f", latitude, longitude))
                } else {
                    Text(if (isArabic) "لم يتم استلام موقع بعد." else "No location received yet.")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = latitude != null && longitude != null,
                onClick = { latitude?.let { lat -> longitude?.let { lon -> onConfirm(lat, lon) } } }
            ) { Text(if (isArabic) "حفظ الموقع" else "Save location") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )
}

private fun parseMapCoordinates(raw: String): Pair<Double, Double>? {
    // Decode nested/escaped Google Maps URLs and HTML entities before inspecting coordinates.
    val value = Uri.decode(raw.trim())
        .replace("&amp;", "&")
        .replace("\\u003d", "=")
        .replace("\\u0026", "&")
    val patterns = listOf(
        // Google Maps place/pin coordinates.
        Regex("!3d(-?\\d{1,2}(?:\\.\\d+)?)!4d(-?\\d{1,3}(?:\\.\\d+)?)"),
        Regex("!2d(-?\\d{1,3}(?:\\.\\d+)?)!3d(-?\\d{1,2}(?:\\.\\d+)?)"),
        // Coordinates embedded in Maps URLs, page metadata, or JSON payloads.
        Regex("@(-?\\d{1,2}(?:\\.\\d+)?),\\s*(-?\\d{1,3}(?:\\.\\d+)?)"),
        Regex("(?:[?&](?:q|query|destination|daddr|center|ll)=)(-?\\d{1,2}(?:\\.\\d+)?)(?:,|%2C)(-?\\d{1,3}(?:\\.\\d+)?)", RegexOption.IGNORE_CASE),
        Regex("\"(?:lat|latitude)\"\\s*:\\s*\"?(-?\\d{1,2}(?:\\.\\d+)?)\"?\\s*[,}]\\s*\"?(?:lng|lon|longitude)\"?\\s*:\\s*\"?(-?\\d{1,3}(?:\\.\\d+)?)", RegexOption.IGNORE_CASE),
        Regex("geo:(-?\\d{1,2}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)", RegexOption.IGNORE_CASE),
        Regex("^\\s*(-?\\d{1,2}(?:\\.\\d+)?)\\s*,\\s*(-?\\d{1,3}(?:\\.\\d+)?)\\s*$")
    )
    for ((index, pattern) in patterns.withIndex()) {
        val match = pattern.find(value) ?: continue
        val first = match.groupValues[1].toDoubleOrNull() ?: continue
        val second = match.groupValues[2].toDoubleOrNull() ?: continue
        // Google Maps !2d/!3d stores longitude before latitude; all other supported forms are lat/lon.
        val lat = if (index == 1) second else first
        val lon = if (index == 1) first else second
        if (lat in -90.0..90.0 && lon in -180.0..180.0) return lat to lon
    }
    return null
}

private fun extractGoogleMapsPlaceName(raw: String, resolvedUrl: String?, page: String?): String? {
    val urlText = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE)
        .find(raw)?.value?.trimEnd('.', ')', ']', ',')
    val uri = runCatching { Uri.parse(resolvedUrl ?: urlText.orEmpty()) }.getOrNull()
    if (uri != null) {
        val segments = uri.pathSegments
        val placeIndex = segments.indexOfFirst { it.equals("place", ignoreCase = true) }
        if (placeIndex >= 0 && segments.size > placeIndex + 1) {
            val candidate = segments[placeIndex + 1].replace('+', ' ').trim()
            if (candidate.isNotBlank() && !candidate.startsWith("@")) return candidate
        }
        for (key in listOf("query", "q", "destination", "daddr")) {
            val candidate = uri.getQueryParameter(key)?.trim().orEmpty()
            if (candidate.isNotBlank() && !candidate.startsWith("place_id:", true) &&
                !Regex("^-?\\d+(?:\\.\\d+)?\\s*,").containsMatchIn(candidate)
            ) return candidate
        }
    }

    val htmlTitle = page?.let { html ->
        val metaTag = Regex("<meta[^>]*property=[\"']og:title[\"'][^>]*>", RegexOption.IGNORE_CASE)
            .find(html)?.value
        metaTag?.let { Regex("content=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
            ?: Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(html)?.groupValues?.get(1)
    }?.let { title ->
        title.replace("&amp;", "&").replace("&#39;", "'").replace("&quot;", "\"")
            .substringBefore(" - Google Maps").trim()
    }
    if (!htmlTitle.isNullOrBlank() && !htmlTitle.equals("Google Maps", true)) return htmlTitle

    // Some share sheets include the selected place's display name before the URL.
    val prefix = if (urlText != null) raw.substringBefore(urlText).trim() else raw.trim()
    return prefix.takeIf { it.isNotBlank() && it.length in 2..160 && !it.contains("http", true) }
}

private suspend fun geocodeSharedMapPlace(
    raw: String,
    resolvedUrl: String?,
    page: String?
): Pair<Double, Double>? = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        val placeName = extractGoogleMapsPlaceName(raw, resolvedUrl, page)
            ?: return@withContext null
        val encoded = java.net.URLEncoder.encode(placeName, "UTF-8")
        val requestUrl = URL("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=$encoded")
        connection = requestUrl.openConnection() as HttpURLConnection
        connection.connectTimeout = 12000
        connection.readTimeout = 12000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "MedicalRepManager/1.0 (Android location lookup)")
        connection.setRequestProperty("Accept", "application/json")
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        val firstResult = Regex("\\{[^{}]*\"lat\"\\s*:\\s*\"(-?\\d+(?:\\.\\d+)?)\"[^{}]*\"lon\"\\s*:\\s*\"(-?\\d+(?:\\.\\d+)?)\"[^{}]*}")
            .find(body) ?: return@withContext null
        val lat = firstResult.groupValues[1].toDoubleOrNull() ?: return@withContext null
        val lon = firstResult.groupValues[2].toDoubleOrNull() ?: return@withContext null
        if (lat in -90.0..90.0 && lon in -180.0..180.0) lat to lon else null
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}

private data class ResolvedMapLink(val url: String, val page: String)

private suspend fun resolveSharedMapLink(raw: String): ResolvedMapLink? = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        val match = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE).find(raw)?.value
            ?: return@withContext null
        connection = URL(match.trimEnd('.', ')', ']', ',')).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*")
        connection.connect()
        val resolvedUrl = connection.url.toString()
        val stream = if (connection.responseCode in 200..399) connection.inputStream else connection.errorStream
        val page = stream?.bufferedReader()?.use { reader ->
            val output = StringBuilder()
            val buffer = CharArray(8192)
            var total = 0
            while (total < 2_000_000) {
                val count = reader.read(buffer, 0, minOf(buffer.size, 2_000_000 - total))
                if (count <= 0) break
                output.append(buffer, 0, count)
                total += count
            }
            output.toString()
        }.orEmpty()
        ResolvedMapLink(resolvedUrl, page)
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}
