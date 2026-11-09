package com.medicalrep.manager.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.layout.padding
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Holds a Google Maps share payload delivered to the app through Android's share sheet.
 * The picker observes this state and converts the shared URL into coordinates.
 */
object MapShareState {
    var sharedText by mutableStateOf<String?>(null)
}

fun handleMapShareIntent(intent: Intent?) {
    if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
        MapShareState.sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
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

    LaunchedEffect(MapShareState.sharedText) {
        val incoming = MapShareState.sharedText
        if (!incoming.isNullOrBlank()) {
            sharedLink = incoming
            val parsed = parseMapCoordinates(incoming)
                ?: resolveSharedMapLink(incoming)?.let { parseMapCoordinates(it) }
            if (parsed != null) {
                latitude = parsed.first
                longitude = parsed.second
                linkError = false
            } else {
                linkError = true
            }
            MapShareState.sharedText = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "تحديد موقع المركز" else "Set center location") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (isArabic) "افتح Google Maps، اضغط مطولًا على موقع المركز، ثم اختر مشاركة واختر Medical Rep Manager. سيعود رابط الموقع إلى هنا تلقائيًا."
                    else "Open Google Maps, long-press the center location, tap Share, then choose Medical Rep Manager. The shared location link will return here automatically."
                )
                OutlinedButton(
                    onClick = { openGoogleMapsLocationPicker(context, latitude, longitude) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isArabic) "فتح Google Maps لاختيار الموقع" else "Open Google Maps to choose location")
                }
                OutlinedTextField(
                    value = sharedLink,
                    onValueChange = { sharedLink = it; linkError = false },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = linkError,
                    label = { Text(if (isArabic) "رابط الموقع المُشارك أو الإحداثيات" else "Shared location link or coordinates") }
                )
                OutlinedButton(
                    onClick = {
                        val parsed = parseMapCoordinates(sharedLink)
                        if (parsed == null) linkError = true
                        else {
                            latitude = parsed.first
                            longitude = parsed.second
                            linkError = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isArabic) "استخراج الموقع من الرابط" else "Read location from link") }
                if (linkError) {
                    Text(
                        if (isArabic) "الرابط لا يحتوي على إحداثيات قابلة للاستخراج. استخدم مشاركة الموقع من Google Maps أو الصق الإحداثيات بصيغة lat, lon."
                        else "The link has no readable coordinates. Share a dropped pin from Google Maps or paste coordinates as lat, lon."
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
    val value = Uri.decode(raw.trim())
    val patterns = listOf(
        Regex("@(-?\\d{1,2}(?:\\.\\d+)?),\\s*(-?\\d{1,3}(?:\\.\\d+)?)"),
        Regex("!3d(-?\\d{1,2}(?:\\.\\d+)?)!4d(-?\\d{1,3}(?:\\.\\d+)?)"),
        Regex("(?:[?&](?:q|query|destination|daddr)=)(-?\\d{1,2}(?:\\.\\d+)?)(?:,|%2C)(-?\\d{1,3}(?:\\.\\d+)?)", RegexOption.IGNORE_CASE),
        Regex("geo:(-?\\d{1,2}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)", RegexOption.IGNORE_CASE),
        Regex("^\\s*(-?\\d{1,2}(?:\\.\\d+)?)\\s*,\\s*(-?\\d{1,3}(?:\\.\\d+)?)\\s*$")
    )
    for (pattern in patterns) {
        val match = pattern.find(value) ?: continue
        val lat = match.groupValues[1].toDoubleOrNull() ?: continue
        val lon = match.groupValues[2].toDoubleOrNull() ?: continue
        if (lat in -90.0..90.0 && lon in -180.0..180.0) return lat to lon
    }
    return null
}


private suspend fun resolveSharedMapLink(raw: String): String? = withContext(Dispatchers.IO) {
    try {
        val match = Regex("https?://\\S+", RegexOption.IGNORE_CASE).find(raw)?.value ?: return@withContext null
        val connection = URL(match).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        connection.connect()
        val resolved = connection.url.toString()
        connection.disconnect()
        resolved
    } catch (_: Exception) {
        null
    }
}
