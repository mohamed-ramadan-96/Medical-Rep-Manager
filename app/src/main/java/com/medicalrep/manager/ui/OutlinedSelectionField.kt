package com.medicalrep.manager.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.medicalrep.manager.ui.theme.AppCardContainerColor
import com.medicalrep.manager.ui.theme.AppSelectionRowColor

@Composable
fun OutlinedSelectionField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = AppSelectionRowColor()
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(10.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "$label: $value",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AppFilledTextFieldColors(containerColor: Color = AppCardContainerColor()): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = containerColor,
    unfocusedContainerColor = containerColor,
    disabledContainerColor = containerColor,
    errorContainerColor = containerColor,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    errorBorderColor = Color.Transparent,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorTextColor = MaterialTheme.colorScheme.onSurface,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorLabelColor = MaterialTheme.colorScheme.error,
    cursorColor = MaterialTheme.colorScheme.primary,
    errorCursorColor = MaterialTheme.colorScheme.error
)


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionPickerSheet(
    title: String,
    allOptionLabel: String,
    allOptionValue: String,
    options: List<Pair<String, String>>,
    searchPlaceholder: String,
    noResultsLabel: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredOptions = remember(options, searchQuery) {
        options.filter { (_, label) ->
            label.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppDialogContainerColor()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = AppFilledTextFieldColors(),
                placeholder = { Text(searchPlaceholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Surface(
                        onClick = { onSelect(allOptionValue) },
                        color = AppSelectionRowColor(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Text(
                            allOptionLabel,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (filteredOptions.isEmpty()) {
                    item {
                        Text(
                            noResultsLabel,
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(filteredOptions, key = { it.first }) { option ->
                        Surface(
                            onClick = { onSelect(option.first) },
                            color = AppSelectionRowColor(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Text(
                                option.second,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
