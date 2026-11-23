package com.medicalrep.manager.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

@Composable
internal fun CurrentLocationButton(
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    onLocation: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(false) }

    fun fetchLocation() {
        loading = true
        val client = LocationServices.getFusedLocationProviderClient(context)
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val priority = if (fineGranted) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        try {
            client.getCurrentLocation(priority, CancellationTokenSource().token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        onLocation(location.latitude, location.longitude)
                        Toast.makeText(context, if (isArabic) "تم تحديد موقعك الحالي" else "Current location captured", Toast.LENGTH_SHORT).show()
                        loading = false
                    } else {
                        client.lastLocation
                            .addOnSuccessListener { last ->
                                loading = false
                                if (last != null) {
                                    onLocation(last.latitude, last.longitude)
                                    Toast.makeText(context, if (isArabic) "تم استخدام آخر موقع معروف" else "Used last known location", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, if (isArabic) "تعذر تحديد الموقع. تأكد من تشغيل خدمات الموقع وحاول مرة أخرى." else "Couldn't get your location. Turn on Location services and try again.", Toast.LENGTH_LONG).show()
                                }
                            }
                            .addOnFailureListener {
                                loading = false
                                Toast.makeText(context, if (isArabic) "تعذر تحديد الموقع الحالي" else "Unable to get current location", Toast.LENGTH_LONG).show()
                            }
                    }
                }
                .addOnFailureListener {
                    loading = false
                    Toast.makeText(context, if (isArabic) "تعذر تحديد الموقع الحالي" else "Unable to get current location", Toast.LENGTH_LONG).show()
                }
        } catch (_: SecurityException) {
            loading = false
            Toast.makeText(context, if (isArabic) "صلاحية الموقع غير متاحة" else "Location permission is unavailable", Toast.LENGTH_LONG).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) fetchLocation()
        else Toast.makeText(context, if (isArabic) "لازم تسمح للتطبيق بالوصول للموقع" else "Location permission is required", Toast.LENGTH_LONG).show()
    }

    OutlinedButton(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
        onClick = {
            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (fine || coarse) fetchLocation()
            else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        },
        enabled = !loading
    ) {
        Text(
            when {
                loading && isArabic -> "جاري تحديد الموقع..."
                loading -> "Getting location..."
                isArabic -> "استخدام موقعي الحالي"
                else -> "Use my current location"
            }
        )
    }
}


@Composable
internal fun AnimatedLocationActions(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)) + expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            expandFrom = Alignment.Top
        ),
        exit = fadeOut(tween(110)) + shrinkVertically(
            animationSpec = tween(140),
            shrinkTowards = Alignment.Top
        ),
        label = "location_actions_transition"
    ) {
        content()
    }
}
