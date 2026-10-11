package com.tehuberz.weather.lite.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tehuberz.weather.lite.R
import com.tehuberz.weather.lite.ui.theme.AdventureTheme

@OptIn(ExperimentalMaterial3Api::class) // for exposed dropdown menu box & outlined text field
@Composable
fun <T> SearchableDropDown(
    modifier: Modifier = Modifier,
    label: String,
    testTag: String,
    options: List<T>,
    searchQuery: TextFieldValue,
    onSearchQueryChanged: (TextFieldValue) -> Unit,
    onOptionSelected: (String) -> Unit,
    onClear: () -> Unit,
    isSelected: Boolean = false,
    initiallyExpanded: Boolean = false,
    initiallySearchMode: Boolean = false,
    optionToString: (T) -> String = { it.toString() },
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    var isSearchMode by remember { mutableStateOf(initiallySearchMode) }
    val focusRequester = remember { FocusRequester() }
    val focusController = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            runCatching { focusRequester.requestFocus() }
            keyboardController?.show()
        }
    }

    Box(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = it
            },
        ) {
            OutlinedTextField(
                modifier =
                    Modifier
                        .focusRequester(focusRequester)
                        .menuAnchor(
                            type = MenuAnchorType.PrimaryEditable,
                            enabled = !isSearchMode,
                        ).fillMaxWidth()
                        .testTag(testTag),
                value = searchQuery,
                readOnly = !isSearchMode,
                onValueChange = {
                    onSearchQueryChanged(it)
                    expanded = true // keep the dropdown open while searching
                },
                label = { Text(label) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            IconButton(onClick = {
                                onClear()
                                expanded = false
                                isSearchMode = false
                                focusController.clearFocus()
                                keyboardController?.hide()
                            }) {
                                Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.cd_clear_selection))
                            }
                        } else {
                            if (isSearchMode) {
                                IconButton(onClick = {
                                    if (searchQuery.text.isNotEmpty()) {
                                        onSearchQueryChanged(TextFieldValue(""))
                                    } else {
                                        isSearchMode = false
                                        expanded = false
                                        focusController.clearFocus()
                                        keyboardController?.hide()
                                    }
                                }) {
                                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.cd_clear_search))
                                }
                            } else {
                                IconButton(onClick = {
                                    isSearchMode = true
                                    expanded = true
                                }) {
                                    Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.cd_search))
                                }
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expanded,
                                modifier = Modifier.menuAnchor(MenuAnchorType.SecondaryEditable, true),
                            )
                        }
                    }
                },
                singleLine = true,
            )
            if (options.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(optionToString(option)) },
                            onClick = {
                                keyboardController?.hide()
                                focusController.clearFocus()
                                isSearchMode = false
                                expanded = false
                                onOptionSelected(optionToString(option))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSearchableDropDown_Interactive() {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    val allOptions = listOf("California", "New York", "Texas", "Florida", "Washington")
    val filteredOptions =
        remember(searchQuery.text) {
            if (searchQuery.text.isEmpty()) {
                allOptions
            } else {
                allOptions.filter { it.contains(searchQuery.text, ignoreCase = true) }
            }
        }

    AdventureTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            SearchableDropDown(
                label = "State",
                testTag = "StateDropdown",
                options = filteredOptions,
                searchQuery = searchQuery,
                onSearchQueryChanged = { searchQuery = it },
                onOptionSelected = {
                    selectedOption = it
                    searchQuery = TextFieldValue(it)
                },
                onClear = {
                    selectedOption = null
                    searchQuery = TextFieldValue("")
                },
                isSelected = selectedOption != null,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSearchableDropDown_Idle() {
    AdventureTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            SearchableDropDown(
                label = "State",
                testTag = "StateDropdown",
                options = listOf("California", "New York", "Texas"),
                searchQuery = TextFieldValue(""),
                onSearchQueryChanged = {},
                onOptionSelected = {},
                onClear = {},
                isSelected = false,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSearchableDropDown_Selected() {
    AdventureTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            SearchableDropDown(
                label = "State",
                testTag = "StateDropdown",
                options = listOf("California", "New York", "Texas"),
                searchQuery = TextFieldValue("California"),
                onSearchQueryChanged = {},
                onOptionSelected = {},
                onClear = {},
                isSelected = true,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSearchableDropDown_Expanded() {
    AdventureTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            SearchableDropDown(
                label = "State",
                testTag = "StateDropdown",
                options = listOf("California", "New York", "Texas"),
                searchQuery = TextFieldValue(""),
                onSearchQueryChanged = {},
                onOptionSelected = {},
                onClear = {},
                isSelected = false,
                initiallyExpanded = true,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSearchableDropDown_SearchMode() {
    AdventureTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            SearchableDropDown(
                label = "State",
                testTag = "StateDropdown",
                options = listOf("California", "Colorado", "Connecticut"),
                searchQuery = TextFieldValue("Cal"),
                onSearchQueryChanged = {},
                onOptionSelected = {},
                onClear = {},
                isSelected = false,
                initiallySearchMode = true,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSearchableDropDown_Dark() {
    AdventureTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            SearchableDropDown(
                label = "State",
                testTag = "StateDropdown",
                options = listOf("California", "New York", "Texas"),
                searchQuery = TextFieldValue("California"),
                onSearchQueryChanged = {},
                onOptionSelected = {},
                onClear = {},
                isSelected = true,
            )
        }
    }
}
