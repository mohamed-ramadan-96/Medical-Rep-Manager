package com.medicalrep.manager.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
fun NotesTextField(
    value: String,
    onValueChange: (String) -> Unit,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    singleLine: Boolean = false,
    minLines: Int = 1
) {
    val context = LocalContext.current
    val currentValue = rememberUpdatedState(value)
    val currentOnValueChange = rememberUpdatedState(onValueChange)
    val dictationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val recognized = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (recognized.isNotBlank()) {
                val latestValue = currentValue.value
                currentOnValueChange.value(
                    if (latestValue.isBlank()) recognized
                    else latestValue.trimEnd() + " " + recognized
                )
            }
        }
    }
    val startDictation: () -> Unit = {
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                if (isArabic) "ar-EG" else Locale.getDefault().toLanguageTag()
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PROMPT, if (isArabic) "تكلم الآن" else "Speak now")
        }
        try {
            dictationLauncher.launch(speechIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                if (isArabic) "التعرف على الصوت غير متاح على الجهاز" else "Speech recognition is not available on this device",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        colors = AppFilledTextFieldColors(),
        shape = RoundedCornerShape(10.dp),
        label = label,
        placeholder = placeholder,
        singleLine = singleLine,
        minLines = minLines,
        trailingIcon = {
            IconButton(onClick = startDictation) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = if (isArabic) "إملاء صوتي" else "Voice dictation"
                )
            }
        }
    )
}
