package com.medicalrep.manager.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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

fun openSavedMapLocation(context: Context, latitude: Double?, longitude: Double?, mapUrl: String?) {
    if (latitude != null && longitude != null) {
        openCenterDirections(context, latitude, longitude)
    } else if (!mapUrl.isNullOrBlank()) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl)))
    }
}

private fun extractSharedMapUrl(raw: String): String? =
    Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE)
        .find(raw)?.value?.trimEnd('.', ')', ']', ',')

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
            shape = MaterialTheme.shapes.medium
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
    initialMapUrl: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (Double?, Double?, String?) -> Unit
) {
    val context = LocalContext.current
    var latitude by remember(initialLatitude, initialLongitude) { mutableStateOf(initialLatitude) }
    var longitude by remember(initialLatitude, initialLongitude) { mutableStateOf(initialLongitude) }
    var mapUrl by remember(initialMapUrl) { mutableStateOf(initialMapUrl) }
    var sharedLink by remember { mutableStateOf("") }
    var linkError by remember { mutableStateOf(false) }
    var resolving by remember { mutableStateOf(false) }

    LaunchedEffect(MapShareState.sharedText) {
        val incoming = MapShareState.sharedText
        if (!incoming.isNullOrBlank()) {
            sharedLink = incoming.trim()
            latitude = null
            longitude = null
            mapUrl = extractSharedMapUrl(incoming)
            linkError = false
            MapShareState.sharedText = null
            resolving = true
        }
    }

    LaunchedEffect(resolving, sharedLink) {
        if (resolving && sharedLink.isNotBlank()) {
            val resolved = resolveSharedMapLink(sharedLink)
            val pageMapUrls = resolved?.page?.let(::extractGoogleMapsUrls).orEmpty()
            val pageMapUrl = pageMapUrls.firstNotNullOfOrNull {
                parseMapCoordinates(it, allowViewport = false)
            }
            val directCoordinates = parseMapCoordinates(sharedLink, allowViewport = false)
                ?: resolved?.let {
                    parseMapCoordinates(it.url, allowViewport = false)
                        ?: parseMapCoordinates(it.page, allowViewport = false)
                }
                ?: pageMapUrl
            val placeName = extractGoogleMapsPlaceName(sharedLink, resolved?.url, resolved?.page)
            val geocoded = if (directCoordinates == null) {
                geocodeSharedMapPlace(sharedLink, resolved?.url, resolved?.page)
            } else null
            val parsed = directCoordinates
                ?: geocoded
                ?: parseMapCoordinates(sharedLink)
                ?: resolved?.let { parseMapCoordinates(it.url) }


            mapUrl = extractSharedMapUrl(sharedLink) ?: sharedLink.trim().takeIf { it.startsWith("http", true) }
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
        title = { Text(if (isArabic) "تحديد الموقع" else "Set location") },
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
                    colors = AppFilledTextFieldColors(),
                    value = sharedLink,
                    onValueChange = {
                        sharedLink = it
                        latitude = null
                        longitude = null
                        mapUrl = extractSharedMapUrl(it)
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
                        if (isArabic) "تعذر استخراج الإحداثيات؛ سيتم حفظ رابط المكان وفتحه مباشرة على Google Maps."
                        else "Couldn't extract coordinates. The place link will be saved and opened directly in Google Maps.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (latitude != null && longitude != null) {
                    Text(String.format(Locale.US, "Latitude: %.6f   Longitude: %.6f", latitude, longitude))
                } else if (!mapUrl.isNullOrBlank()) {
                    Text(if (isArabic) "تم الاحتفاظ برابط المكان. سيُفتح مباشرة في Google Maps." else "The place link is saved and will open directly in Google Maps.")
                } else {
                    Text(if (isArabic) "لم يتم استلام موقع بعد." else "No location received yet.")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = (latitude != null && longitude != null) || !mapUrl.isNullOrBlank(),
                onClick = { onConfirm(latitude, longitude, mapUrl) }
            ) { Text(if (isArabic) "حفظ الموقع" else "Save location") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )
}

private fun parseMapCoordinates(raw: String, allowViewport: Boolean = true): Pair<Double, Double>? {
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
        Regex("(?:[?&](?:q|query|destination|daddr|center|ll)=)(-?\\d{1,2}(?:\\.\\d+)?)(?:,|%2C)(-?\\d{1,3}(?:\\.\\d+)?)", RegexOption.IGNORE_CASE),
        Regex("\"(?:lat|latitude)\"\\s*:\\s*\"?(-?\\d{1,2}(?:\\.\\d+)?)\"?\\s*[,}]\\s*\"?(?:lng|lon|longitude)\"?\\s*:\\s*\"?(-?\\d{1,3}(?:\\.\\d+)?)", RegexOption.IGNORE_CASE),
        Regex("geo:(-?\\d{1,2}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)", RegexOption.IGNORE_CASE),
        Regex("^\\s*(-?\\d{1,2}(?:\\.\\d+)?)\\s*,\\s*(-?\\d{1,3}(?:\\.\\d+)?)\\s*$")
    ) + if (allowViewport) listOf(
        Regex("@(-?\\d{1,2}(?:\\.\\d+)?),\\s*(-?\\d{1,3}(?:\\.\\d+)?)")
    ) else emptyList()
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

private fun extractGoogleMapsUrls(page: String): List<String> {
    val pattern = Regex(
        "https?:(?:\\\\/\\\\/|//)(?:www\\.)?(?:google\\.[^/\\s\"'<>]+/maps[^\\s\"'<>]*|maps\\.google\\.[^/\\s\"'<>]+/[^\\s\"'<>]*)",
        RegexOption.IGNORE_CASE
    )
    return pattern.findAll(page)
        .map { it.value.replace("\\\\/", "/").replace("&amp;", "&").trimEnd(',', ';', ')') }
        .distinct()
        .toList()
}

private fun extractGoogleMapsPlaceName(raw: String, resolvedUrl: String?, page: String?): String? {
    val rawUrl = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE)
        .find(raw)?.value?.trimEnd('.', ')', ']', ',')
    val candidateUrls = buildList {
        if (!resolvedUrl.isNullOrBlank()) add(resolvedUrl)
        if (!rawUrl.isNullOrBlank()) add(rawUrl)
        if (!page.isNullOrBlank()) addAll(extractGoogleMapsUrls(page))
    }

    for (candidateUrl in candidateUrls.distinct()) {
        val uri = runCatching { Uri.parse(candidateUrl) }.getOrNull() ?: continue
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
        val metaTitle = Regex(
            "<meta[^>]+(?:property|name)=[\"'](?:og:title|twitter:title)[\"'][^>]+content=[\"']([^\"']+)[\"']",
            RegexOption.IGNORE_CASE
        ).find(html)?.groupValues?.get(1)
        metaTitle ?: Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.get(1)
    }?.let { title ->
        title.replace("&amp;", "&").replace("&#39;", "'").replace("&quot;", "\"")
            .substringBefore(" - Google Maps").trim()
    }
    if (!htmlTitle.isNullOrBlank() && !htmlTitle.equals("Google Maps", true)) return htmlTitle

    val prefix = if (rawUrl != null) raw.substringBefore(rawUrl).trim() else raw.trim()
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

private data class ResolvedMapLink(
    val url: String,
    val page: String,
    val statusCode: Int? = null,
    val error: String? = null
)

private suspend fun resolveSharedMapLink(raw: String): ResolvedMapLink? = withContext(Dispatchers.IO) {
    var currentUrl = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE)
        .find(raw)?.value?.trimEnd('.', ')', ']', ',')
        ?: return@withContext null
    var page = ""
    var lastStatusCode: Int? = null
    try {
        // Resolve Google short links one redirect at a time and retain diagnostics.
        repeat(12) {
            var connection: HttpURLConnection? = null
            try {
                connection = URL(currentUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
                )
                connection.setRequestProperty(
                    "Accept",
                    "text/html,application/xhtml+xml,application/json,*/*"
                )
                connection.setRequestProperty("Accept-Language", "ar,en-US;q=0.9,en;q=0.8")
                connection.connect()

                val status = connection.responseCode
                lastStatusCode = status
                val location = connection.getHeaderField("Location")
                if (status in 300..399 && !location.isNullOrBlank()) {
                    currentUrl = URL(URL(currentUrl), location).toString()
                    return@repeat
                }

                val stream = if (status in 200..399) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                page = stream?.bufferedReader()?.use { reader ->
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
                return@withContext ResolvedMapLink(currentUrl, page, lastStatusCode)
            } finally {
                connection?.disconnect()
            }
        }
        ResolvedMapLink(currentUrl, page, lastStatusCode)
    } catch (error: Exception) {
        ResolvedMapLink(
            currentUrl,
            page,
            lastStatusCode,
            "${error.javaClass.simpleName}: ${error.message ?: "no message"}"
        )
    }
}
