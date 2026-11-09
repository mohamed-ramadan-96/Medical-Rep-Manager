package com.medical.rep.manager.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

@Composable
internal fun CurrentLocationButton(
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    onLocation: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(false) }

    fun showMessage(arabic: String, english: String) {
        Toast.makeText(context, if (isArabic) arabic else english, Toast.LENGTH_LONG).show()
    }

    fun fetchLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted) {
            showMessage(
                "لازم تسمح بالوصول إلى الموقع الدقيق (Precise location) علشان نحدد موقعك الحالي.",
                "Precise location permission is required to get your current location."
            )
            return
        }

        loading = true
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cancellation = CancellationTokenSource()
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0L)
            .setDurationMillis(20_000L)
            .build()

        try {
            client.getCurrentLocation(request, cancellation.token)
                .addOnSuccessListener { location ->
                    loading = false
                    when {
                        location == null -> showMessage(
                            "تعذر الحصول على قراءة GPS جديدة. تأكد من تشغيل خدمات الموقع وحاول مرة أخرى.",
                            "Couldn't get a fresh GPS reading. Check that Location is enabled and try again."
                        )
                        !location.hasAccuracy() || location.accuracy > 50f -> showMessage(
                            "الموقع المستلم غير دقيق بما يكفي (\${if (location.hasAccuracy()) location.accuracy.toInt() else "—"} متر). حاول مرة أخرى في مكان مفتوح.",
                            "The location reading isn't accurate enough (\${if (location.hasAccuracy()) location.accuracy.toInt() else "—"} m). Please try again in an open area."
                        )
                        else -> {
                            onLocation(location.latitude, location.longitude)
                            Toast.makeText(
                                context,
                                if (isArabic) "تم تحديد موقعك الحالي بدقة \${location.accuracy.toInt()} متر" else "Current location captured (±\${location.accuracy.toInt()} m)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    cancellation.cancel()
                }
                .addOnFailureListener {
                    loading = false
                    cancellation.cancel()
                    showMessage(
                        "تعذر تحديد الموقع الحالي. تأكد من تشغيل GPS وحاول مرة أخرى.",
                        "Unable to get your current location. Check GPS and try again."
                    )
                }
        } catch (_: SecurityException) {
            loading = false
            cancellation.cancel()
            showMessage(
                "صلاحية الموقع الدقيق غير متاحة.",
                "Precise location permission is unavailable."
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            fetchLocation()
        } else {
            showMessage(
                "لازم تسمح بالوصول إلى الموقع الدقيق علشان نحدد مكانك الحالي.",
                "Precise location permission is required."
            )
        }
    }

    OutlinedButton(
        modifier = modifier.fillMaxWidth(),
        onClick = {
            val fine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (fine) {
                fetchLocation()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        },
        enabled = !loading
    ) {
        Text(
            when {
                loading && isArabic -> "جاري تحديد موقع GPS..."
                loading -> "Getting fresh GPS location..."
                isArabic -> "استخدام موقعي الحالي"
                else -> "Use my current location"
            }
        )
    }
}
