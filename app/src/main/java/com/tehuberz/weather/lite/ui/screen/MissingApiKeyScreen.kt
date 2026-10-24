package com.tehuberz.weather.lite.ui.screen

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.tehuberz.weather.lite.R
import com.tehuberz.weather.lite.ui.theme.AdventureTheme
import com.tehuberz.weather.lite.util.ApiServiceHost

const val TAG_MISSING_API_KEY_SCREEN = "MissingApiKeyScreen"
const val TAG_GET_API_KEY_BUTTON = "GetApiKeyButton"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingApiKeyScreen(
    host: ApiServiceHost = ApiServiceHost.getActive(),
    onGetApiKeyClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val openPortalAction =
        onGetApiKeyClick ?: {
            val intent = Intent(Intent.ACTION_VIEW, host.apiKeyPortalUrl.toUri())
            context.startActivity(intent)
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scaffold_weather_app_label)) },
            )
        },
    ) { innerPadding ->
        MissingApiKeyScreenContent(
            modifier = Modifier.padding(innerPadding),
            apiKeyConfigName = host.apiKeyConfigName,
            onGetApiKeyClick = openPortalAction,
        )
    }
}

@Composable
fun MissingApiKeyScreenContent(
    modifier: Modifier = Modifier,
    apiKeyConfigName: String = ApiServiceHost.OPEN_WEATHER_MAP.apiKeyConfigName,
    onGetApiKeyClick: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp)
                .testTag(TAG_MISSING_API_KEY_SCREEN),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Key,
            contentDescription = stringResource(R.string.cd_missing_api_key),
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.missing_api_key_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.missing_api_key_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.missing_api_key_steps_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.missing_api_key_step_1),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.missing_api_key_step_2),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = stringResource(R.string.missing_api_key_snippet, apiKeyConfigName),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.missing_api_key_step_3),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onGetApiKeyClick,
            modifier = Modifier.testTag(TAG_GET_API_KEY_BUTTON),
        ) {
            Icon(
                imageVector = Icons.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = stringResource(R.string.missing_api_key_get_key_button))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewMissingApiKeyScreen() {
    AdventureTheme {
        Surface {
            MissingApiKeyScreenContent()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewMissingApiKeyScreenDark() {
    AdventureTheme(darkTheme = true) {
        Surface {
            MissingApiKeyScreenContent()
        }
    }
}
