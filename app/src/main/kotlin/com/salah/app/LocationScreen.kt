package com.salah.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salah.designsystem.SalahCard
import com.salah.designsystem.SectionHeading
import com.salah.location.LocationFailure
import com.salah.location.LocationMode
import com.salah.location.LocationPermission
import com.salah.location.LocationSearchResult
import com.salah.location.SalahLocation
import com.salah.location.locationPermission

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LocationOnboarding(useCurrent: () -> Unit, openPicker: () -> Unit) {
    SalahCard(emphasized = true) {
        Icon(SalahIcons.Place, null, Modifier.size(27.dp), tint = MaterialTheme.colorScheme.primary)
        SectionHeading(stringResource(R.string.location_onboarding_title))
        LocationDetail(stringResource(R.string.location_onboarding_body))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = useCurrent) { Text(stringResource(R.string.use_current_location)) }
            TextButton(
                onClick = openPicker,
            ) { Text(stringResource(R.string.choose_city)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LocationScreen(
    selected: SalahLocation?,
    viewModel: LocationViewModel,
    detectOnEntry: Boolean,
    onSelected: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.hostActivity() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val focus = LocalFocusManager.current
    val action by viewModel.action.collectAsStateWithLifecycle()
    val search by viewModel.results.collectAsStateWithLifecycle()
    val permissions = remember(context) { context.getSharedPreferences("location-permission", Context.MODE_PRIVATE) }
    var permission by remember { mutableStateOf(context.locationPermission()) }
    var permissionBlocked by remember { mutableStateOf(permissions.getBoolean("blocked", false)) }
    var rationale by remember { mutableStateOf(activity.needsLocationRationale()) }
    var entryHandled by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permission = context.locationPermission()
        rationale = activity.needsLocationRationale()
        permissionBlocked = permission == LocationPermission.NONE && !rationale
        permissions.edit { putBoolean("blocked", permissionBlocked) }
        viewModel.detect(permission)
    }
    val locked = permissionBlocked && permission == LocationPermission.NONE && !rationale
    val detect = {
        focus.clearFocus()
        val access = context.locationPermission()
        if (access != LocationPermission.NONE) {
            viewModel.detect(access)
        } else if (locked) {
            viewModel.detect(LocationPermission.NONE)
        } else {
            launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    LaunchedEffect(detectOnEntry) {
        if (detectOnEntry && !entryHandled) {
            entryHandled = true
            detect()
        }
    }
    LaunchedEffect(query) { viewModel.search(query) }
    LaunchedEffect(action.completed) {
        if (action.completed) {
            viewModel.consumeCompletion()
            onSelected()
        }
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    permission = context.locationPermission()
                    rationale = activity.needsLocationRationale()
                    if (permission != LocationPermission.NONE && permissionBlocked) {
                        permissionBlocked = false
                        permissions.edit { remove("blocked") }
                    }
                }
                Lifecycle.Event.ON_STOP -> viewModel.cancelDetection()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.resetPicker()
        }
    }

    ScreenContent {
        selected?.let { currentLoc ->
            SalahCard(emphasized = false, inset = 14.dp, gap = 6.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = currentLoc.displayLabel(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    LocationDetail(currentLoc.sourceAndZone())
                    if (!locked) {
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = detect,
                            enabled = !action.detecting && !action.saving,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(stringResource(R.string.use_current_location))
                        }
                    }
                }
                if (action.detecting || action.saving) {
                    Spacer(Modifier.height(4.dp))
                    StatusText(if (action.detecting) R.string.detecting_location else R.string.saving_location)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                if (locked || action.failure == LocationFailure.PERMISSION) {
                    StatusText(if (locked) R.string.location_blocked else R.string.location_denied)
                    if (locked) TextButton(onClick = { context.openAppSettings() }) {
                        Text(stringResource(R.string.open_settings))
                    }
                } else if (action.failure == LocationFailure.DISABLED) {
                    StatusText(R.string.location_services_off)
                    TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }) {
                        Text(stringResource(R.string.open_location_services))
                    }
                } else if (action.failure == LocationFailure.NO_FIX) {
                    StatusText(R.string.location_no_fix)
                } else if (action.failure == LocationFailure.TIMEZONE) {
                    StatusText(R.string.location_timezone_error)
                }
                action.stale?.let {
                    StatusText(R.string.location_stale_message)
                    Text(it.displayLabel(), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = viewModel::useLastKnown) { Text(stringResource(R.string.use_last_location)) }
                }
                if (action.saveError) StatusText(R.string.location_save_error)
            }
        } ?: run {
            SalahCard(emphasized = false, inset = 14.dp) {
                SectionHeading(stringResource(R.string.find_location))
                LocationDetail(stringResource(R.string.current_location_explanation))
                if (!locked) {
                    FilledTonalButton(
                        onClick = detect,
                        enabled = !action.detecting && !action.saving,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(stringResource(R.string.use_current_location))
                    }
                }
                if (action.detecting || action.saving) {
                    StatusText(if (action.detecting) R.string.detecting_location else R.string.saving_location)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                if (locked || action.failure == LocationFailure.PERMISSION) {
                    StatusText(if (locked) R.string.location_blocked else R.string.location_denied)
                    if (locked) TextButton(onClick = { context.openAppSettings() }) {
                        Text(stringResource(R.string.open_settings))
                    }
                } else if (action.failure == LocationFailure.DISABLED) {
                    StatusText(R.string.location_services_off)
                    TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }) {
                        Text(stringResource(R.string.open_location_services))
                    }
                } else if (action.failure == LocationFailure.NO_FIX) {
                    StatusText(R.string.location_no_fix)
                } else if (action.failure == LocationFailure.TIMEZONE) {
                    StatusText(R.string.location_timezone_error)
                }
                action.stale?.let {
                    StatusText(R.string.location_stale_message)
                    Text(it.displayLabel(), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = viewModel::useLastKnown) { Text(stringResource(R.string.use_last_location)) }
                }
                if (action.saveError) StatusText(R.string.location_save_error)
            }
        }

        SectionHeading(stringResource(R.string.choose_city))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(120) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.search_city)) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = if (query.isNotEmpty()) {
                { IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Clear, stringResource(R.string.clear_search)) } }
            } else null,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.Transparent,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        )

        val currentSearch = search?.takeIf { it.first == query }?.second
        when {
            query.trim().length < 2 -> LocationDetail(stringResource(R.string.search_hint))
            currentSearch == CitySearchState.Error -> SalahCard {
                StatusText(R.string.search_failed)
                TextButton(onClick = viewModel::retrySearch) { Text(stringResource(R.string.retry)) }
            }
            currentSearch is CitySearchState.Results -> {
                if (currentSearch.cities.isEmpty()) StatusText(R.string.search_empty)
                else SalahCard(inset = 0.dp, gap = 0.dp) {
                    Column(Modifier.selectableGroup()) {
                        currentSearch.cities.forEachIndexed { index, city ->
                            if (index > 0) HorizontalDivider(
                                Modifier.padding(start = 48.dp, end = 14.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            )
                            CityRow(city, selected?.id == city.id, !action.saving) {
                                focus.clearFocus()
                                viewModel.choose(city)
                            }
                        }
                    }
                }
            }
            else -> {
                StatusText(R.string.searching_cities)
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CityRow(city: LocationSearchResult, isSelected: Boolean, enabled: Boolean, choose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val isChosen = isSelected
    val rowTone = if (isChosen) colors.primary.copy(alpha = 0.05f) else colors.surface
    Row(
        Modifier.fillMaxWidth().heightIn(min = 54.dp)
            .background(rowTone)
            .selectable(isSelected, enabled = enabled, role = Role.RadioButton, onClick = choose)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(SalahIcons.Place, null, Modifier.size(21.dp), tint = colors.primary)
        Column(Modifier.weight(1f)) {
            Text(city.names.displayName.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            if (city.names.qualifier.isNotBlank()) LocationDetail(city.names.qualifier)
        }
        if (isSelected) Icon(Icons.Outlined.Check, null, Modifier.size(20.dp), tint = colors.primary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LocationSettings(
    selection: LocationSelection,
    viewModel: LocationViewModel,
    openLocation: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var permission by remember { mutableStateOf(context.locationPermission()) }
    var confirming by rememberSaveable { mutableStateOf(false) }
    val action by viewModel.action.collectAsStateWithLifecycle()
    val selected = selection.value
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permission = context.locationPermission()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    SectionHeading(stringResource(R.string.location))
    SalahCard {
        Text(selected?.displayLabel() ?: stringResource(
            if (selection.ready) R.string.no_location else R.string.loading_location,
        ), style = MaterialTheme.typography.titleMedium)
        selected?.let {
            LocationDetail(it.sourceAndZone())
            if (it.mode == LocationMode.AUTOMATIC && permission == LocationPermission.NONE) {
                LocationDetail(stringResource(R.string.location_saved_permission))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { openLocation(false) }, enabled = !action.saving) {
                Text(stringResource(R.string.change_location))
            }
            if (selected?.mode == LocationMode.AUTOMATIC) {
                TextButton(onClick = { openLocation(true) }, enabled = !action.saving) {
                    Text(stringResource(R.string.refresh_current_location))
                }
            }
            if (selected != null || action.saveError) {
                TextButton(onClick = { confirming = true }, enabled = !action.saving) {
                    Text(stringResource(R.string.clear_location))
                }
            }
        }
        if (action.saveError) StatusText(R.string.location_save_error)
        LocationDetail(stringResource(R.string.location_privacy_info))
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.clear_location)) },
            text = { Text(stringResource(R.string.clear_location_detail)) },
            confirmButton = {
                TextButton(onClick = { confirming = false; viewModel.clear() }) {
                    Text(stringResource(R.string.clear_location))
                }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
internal fun SalahLocation.displayLabel(): String {
    val name = names.displayName ?: return stringResource(
        if (accuracy?.approximate == true) R.string.approximate_area else R.string.detected_location,
    )
    return names.region?.takeIf { it != name }?.let { stringResource(R.string.place_and_region, name, it) } ?: name
}

@Composable
internal fun SalahLocation.statusLabel(): String? = when {
    stale -> stringResource(R.string.saved_location)
    mode == LocationMode.MANUAL -> stringResource(R.string.manual_city)
    accuracy?.approximate == true && names.displayName != null -> stringResource(R.string.approximate_short)
    else -> null
}

@Composable
private fun SalahLocation.sourceAndZone(): String {
    val modeLabel = stringResource(if (mode == LocationMode.AUTOMATIC) R.string.automatic else R.string.manual)
    val quality = if (mode == LocationMode.AUTOMATIC) statusLabel() else null
    return listOfNotNull(modeLabel, quality, zone.id).joinToString(" · ")
}

@Composable
private fun LocationDetail(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun StatusText(message: Int) {
    Text(
        stringResource(message), Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium,
    )
}

private fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
}

private fun Context.hostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.hostActivity()
    else -> null
}

private fun Activity?.needsLocationRationale() = this != null &&
    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)
