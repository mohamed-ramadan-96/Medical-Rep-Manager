package com.medicalrep.manager.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale

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
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webViewClient = WebViewClient()
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun picked(lat: Double, lon: Double) {
                            onLocationPicked?.invoke(lat, lon)
                        }
                    },
                    "AndroidMap"
                )
                val lat = latitude?.toString() ?: "26.8"
                val lon = longitude?.toString() ?: "30.8"
                val zoom = if (latitude != null && longitude != null) 16 else 5
                val initialMarker = if (latitude != null && longitude != null) "place($lat,$lon);" else ""
                val html = """
                    <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
                    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                    <style>html,body,#map{height:100%;width:100%;margin:0;padding:0} .leaflet-control-attribution{font-size:8px}</style>
                    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script></head><body><div id="map"></div>
                    <script>
                    const map=L.map('map').setView([$lat,$lon],$zoom);
                    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap'}).addTo(map);
                    let marker=null;
                    function place(lat,lon){
                      if(marker){map.removeLayer(marker);}
                      marker=L.marker([lat,lon]).addTo(map);
                    }
                    $initialMarker
                    map.on('contextmenu',function(e){
                      place(e.latlng.lat,e.latlng.lng);
                      if(window.AndroidMap){AndroidMap.picked(e.latlng.lat,e.latlng.lng);}
                    });
                    </script></body></html>
                """.trimIndent()
                loadDataWithBaseURL("https://local.map/", html, "text/html", "UTF-8", null)
            }
        }
    )
}

@Composable
fun CenterMapPickerDialog(
    isArabic: Boolean,
    initialLatitude: Double?,
    initialLongitude: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit
) {
    var latitude by remember(initialLatitude, initialLongitude) { mutableStateOf(initialLatitude) }
    var longitude by remember(initialLatitude, initialLongitude) { mutableStateOf(initialLongitude) }
    var sharedLink by remember { mutableStateOf("") }
    var linkError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "تحديد موقع المركز" else "Set center location") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (isArabic) "اضغط مطولًا على موقع المركز في الخريطة لوضع العلامة وحفظ الإحداثيات."
                    else "Long-press the center location on the map to place a pin and save its coordinates."
                )
                CenterMapPreview(
                    latitude = latitude,
                    longitude = longitude,
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    onLocationPicked = { lat, lon -> latitude = lat; longitude = lon }
                )
                OutlinedTextField(
                    value = sharedLink,
                    onValueChange = { sharedLink = it; linkError = false },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = linkError,
                    label = { Text(if (isArabic) "أو الصق رابط Google Maps / الإحداثيات" else "Or paste a Google Maps link / coordinates") }
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
                        if (isArabic) "لم أتمكن من استخراج الإحداثيات. جرّب نسخ رابط يحتوي على دبوس الموقع أو اكتب الإحداثيات بصيغة lat, lon."
                        else "Could not read coordinates. Paste a pin link or enter coordinates as lat, lon."
                    )
                }
                if (latitude != null && longitude != null) {
                    Text(String.format(Locale.US, "Latitude: %.6f   Longitude: %.6f", latitude, longitude))
                } else {
                    Text(if (isArabic) "لم يتم تحديد موقع بعد." else "No location selected yet.")
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
