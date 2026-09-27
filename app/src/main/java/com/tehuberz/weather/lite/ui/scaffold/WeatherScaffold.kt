package com.tehuberz.weather.lite.ui.scaffold

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tehuberz.weather.lite.R
import com.tehuberz.weather.lite.data.local.model.Bookmark
import com.tehuberz.weather.lite.ui.state.WeatherUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScaffold(
    uiState: WeatherUiState,
    snackBarHostState: SnackbarHostState,
    onRemoveBookmark: (Bookmark) -> Unit,
    onLoadBookmark: (Bookmark) -> Unit,
    onSettingsClick: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    var showBookmarks by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scaffold_weather_app_label)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.cd_settings))
                    }
                    IconButton(onClick = { showBookmarks = true }) {
                        Icon(Icons.Filled.Bookmark, contentDescription = stringResource(R.string.cd_bookmarks))
                    }
                },
            )
        },
        content = content,
        snackbarHost = {
            SnackbarHost(snackBarHostState)
        },
    )

    if (showBookmarks) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showBookmarks = false },
            sheetState = sheetState,
        ) {
            BookmarksList(
                bookmarks = uiState.bookmarks,
                onBookmarkClick = {
                    onLoadBookmark(it)
                    showBookmarks = false
                },
                onDeleteClick = onRemoveBookmark,
            )
        }
    }
}

@Composable
private fun BookmarksList(
    bookmarks: List<Bookmark>,
    onBookmarkClick: (Bookmark) -> Unit,
    onDeleteClick: (Bookmark) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(
            text = stringResource(R.string.scaffold_bookmarked_locations_label),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        )
        if (bookmarks.isEmpty()) {
            Text(
                text = stringResource(R.string.scaffold_no_bookmarks_yet_label),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            bookmarks.forEach { bookmark ->
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("${bookmark.cityName}, ${bookmark.stateAbbreviation}") },
                    leadingContent = {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { onDeleteClick(bookmark) }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.cd_delete_bookmark))
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onBookmarkClick(bookmark) },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewScaffold() {
    WeatherScaffold(
        uiState = WeatherUiState(),
        onRemoveBookmark = {},
        onLoadBookmark = {},
        onSettingsClick = {},
        content = {},
        snackBarHostState = remember { SnackbarHostState() },
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewBookmarksList(
    @PreviewParameter(BookmarkPreviewParameterProvider::class) bookmarks: List<Bookmark>,
) {
    BookmarksList(
        bookmarks = bookmarks,
        onBookmarkClick = {},
        onDeleteClick = {},
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewBookmarksListEmpty() {
    BookmarksList(
        bookmarks = emptyList(),
        onBookmarkClick = {},
        onDeleteClick = {},
    )
}

private class BookmarkPreviewParameterProvider : PreviewParameterProvider<List<Bookmark>> {
    override val values =
        sequenceOf(
            listOf(
                Bookmark(
                    id = 1,
                    stateName = "Georgia",
                    stateAbbreviation = "GA",
                    cityName = "Atlanta",
                ),
                Bookmark(
                    id = 2,
                    stateName = "New York",
                    stateAbbreviation = "NY",
                    cityName = "New York",
                ),
                Bookmark(
                    id = 3,
                    stateName = "California",
                    stateAbbreviation = "CA",
                    cityName = "Los Angeles",
                ),
            ),
        )
}
