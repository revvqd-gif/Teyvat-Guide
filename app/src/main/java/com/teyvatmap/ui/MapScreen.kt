package com.teyvatmap.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.teyvatmap.R
import com.teyvatmap.data.LabelNode
import com.teyvatmap.map.TeyvatMapView
import kotlinx.coroutines.launch
import io.coil.compose.rememberAsyncImagePainter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.input.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onLogout: () -> Unit
) {
    val showSidebar by remember { mutableStateOf(false) }
    val showCookieDialog by remember { mutableStateOf(false) }
    val showError by remember { mutableStateOf(false) }
    val errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Observe view model state
    val mapInfo by viewModel.mapInfo.collectAsStateWithLifecycle()
    val labelTree by viewModel.labelTree.collectAsStateWithLifecycle()
    val areas by viewModel.areas.collectAsStateWithLifecycle()
    val points by viewModel.points.collectAsStateWithLifecycle()
    val markedPoints by viewModel.markedPoints.collectAsStateWithLifecycle()
    val selectedLabelIds by viewModel.selectedLabelIds.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val cookieStatus by viewModel.cookieStatus.collectAsStateWithLifecycle()
    val hasValidCookie by viewModel.hasValidCookie.collectAsStateWithLifecycle()
    val showZones by viewModel.showZones.collectAsStateWithLifecycle()
    val onlyUncollected by viewModel.onlyUncollected.collectAsStateWithLifecycle()

    // Handle error display
    androidx.compose.runtime.LaunchedEffect(viewModel.errorMessage.collectAsStateWithLifecycle()) {
        val msg = viewModel.errorMessage.value
        if (msg != null) {
            errorMessage.value = msg
            showError.value = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map View
        TeyvatMapView(
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel
        )

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
            }
        }

        // Top App Bar
        TopAppBar(
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(),
            title = {
                Text("Teyvat Map", fontWeight = FontWeight.Bold)
            },
            colors = TopAppBarDefaults.mediumTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)
            ),
            navigationIcon = {
                IconButton(onClick = { showSidebar.value = true }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.toggleUncollected() }) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = if (onlyUncollected) "Show all" else "Show uncollected only",
                        tint = if (onlyUncollected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.toggleZones() }) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = if (showZones) "Hide zones" else "Show zones",
                        tint = if (showZones) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.refreshPoints() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh points")
                }
                if (hasValidCookie) {
                    IconButton(onClick = { viewModel.refreshMarks() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync marks")
                    }
                }
                IconButton(onClick = { showCookieDialog.value = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )

        // Sidebar
        if (showSidebar) {
            Sidebar(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                viewModel = viewModel,
                onClose = { showSidebar.value = false },
                cookieStatus = cookieStatus,
                hasValidCookie = hasValidCookie,
                onCookieClick = { showCookieDialog.value = true },
                onSyncClick = { viewModel.refreshMarks() },
                onRefreshClick = { viewModel.refreshPoints() },
                onLogoutClick = onLogout,
                showZones = showZones,
                onZoneToggle = { viewModel.toggleZones() },
                onlyUncollected = onlyUncollected,
                onUncollectedToggle = { viewModel.toggleUncollected() }
            )
        }

        // Cookie Dialog
        if (showCookieDialog) {
            CookieDialog(
                onDismiss = { showCookieDialog.value = false },
                onSave = { cookie ->
                    viewModel.saveCookie(cookie)
                    showCookieDialog.value = false
                },
                onLogout = {
                    viewModel.clearCookie()
                    showCookieDialog.value = false
                },
                hasCookie = hasValidCookie
            )
        }

        // Error Dialog
        if (showError) {
            ErrorDialog(
                message = errorMessage.value!!,
                onDismiss = {
                    showError.value = false
                    errorMessage.value = null
                }
            )
        }
    }
}

@Composable
fun Sidebar(
    modifier: Modifier,
    viewModel: MapViewModel,
    onClose: () -> Unit,
    cookieStatus: String,
    hasValidCookie: Boolean,
    onCookieClick: () -> Unit,
    onSyncClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onLogoutClick: () -> Unit,
    showZones: Boolean,
    onZoneToggle: () -> Unit,
    onlyUncollected: Boolean,
    onUncollectedToggle: () -> Unit
) {
    val labelTree by viewModel.labelTree.collectAsStateWithLifecycle()
    val selectedLabelIds by viewModel.selectedLabelIds.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .wrapContentSize(Alignment.CenterStart),
        contentAlignment = Alignment.CenterStart
    ) {
        Card(
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Teyvat Map", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))

                // Account Section
                AccountSection(
                    cookieStatus = cookieStatus,
                    hasValidCookie = hasValidCookie,
                    onCookieClick = onCookieClick,
                    onSyncClick = onSyncClick,
                    onRefreshClick = onRefreshClick,
                    onLogoutClick = onLogoutClick
                )

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Map Layers
                MapLayersSection(
                    showZones = showZones,
                    onZoneToggle = onZoneToggle,
                    onlyUncollected = onlyUncollected,
                    onUncollectedToggle = onUncollectedToggle
                )

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Category Filters
                Text("Categories", style = MaterialTheme.typography.titleMedium)

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Top
                ) {
                    verticalScroll(rememberScrollState()) {
                        Column {
                            labelTree?.fold(
                                onSuccess = { tree ->
                                    tree.forEach { category ->
                                        CategoryItem(
                                            category = category,
                                            selectedIds = selectedLabelIds,
                                            onToggle = { id, checked ->
                                                viewModel.toggleLabel(id)
                                            },
                                            getChildLabels = { viewModel.getChildLabels(it) }
                                        )
                                    }
                                },
                                onFailure = { _, _ ->
                                    Text("Failed to load categories", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                },
                                onLoading = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
                                    )
                                },
                                onIdle = {
                                    Text("No categories", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountSection(
    cookieStatus: String,
    hasValidCookie: Boolean,
    onCookieClick: () -> Unit,
    onSyncClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Account", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onCookieClick) {
                Icon(Icons.Default.Settings, contentDescription = "Cookie Settings")
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (hasValidCookie) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (hasValidCookie) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 8.dp))
            Text(cookieStatus, style = MaterialTheme.typography.bodyMedium)
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSyncClick,
                modifier = Modifier.weight(1f),
                enabled = hasValidCookie
            ) { Text("Sync Marks") }
            Button(
                onClick = onRefreshClick,
                modifier = Modifier.weight(1f)
            ) { Text("Refresh") }
            if (hasValidCookie) {
                Button(
                    onClick = onLogoutClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) { Text("Logout") }
            }
        }
    }
}

@Composable
fun MapLayersSection(
    showZones: Boolean,
    onZoneToggle: () -> Unit,
    onlyUncollected: Boolean,
    onUncollectedToggle: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Map Layers", style = MaterialTheme.typography.titleMedium)
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = showZones,
                onCheckedChange = { onZoneToggle() },
                modifier = Modifier.weight(1f).fillMaxWidth().wrapContentSize()
            )
            Text("Region Boundaries", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).fillMaxWidth().wrapContentSize())

            Checkbox(
                checked = onlyUncollected,
                onCheckedChange = { onUncollectedToggle() },
                modifier = Modifier.weight(1f).fillMaxWidth().wrapContentSize()
            )
            Text("Uncollected Only", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).fillMaxWidth().wrapContentSize())
        }
    }
}

@Composable
fun CategoryItem(
    category: LabelNode,
    selectedIds: Set<Int>,
    onToggle: (Int, Boolean) -> Unit,
    getChildLabels: (Int) -> List<LabelNode>
) {
    val isExpanded = remember { mutableStateOf(false) }
    val hasChildren = category.children?.isNotEmpty() == true
    val isSelected = category.id in selectedIds
    val childLabels = remember(category.id) { getChildLabels(category.id) }
    val hasCachedChildren = childLabels.isNotEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    category.icon?.let {
                        Image(
                            painter = rememberAsyncImagePainter(it),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 8.dp))
                    Column {
                        Text(category.name, style = MaterialTheme.typography.bodyMedium)
                        val count = (category.children?.size ?: 0) + (if (hasCachedChildren) childLabels.size else 0)
                        if (count > 0) {
                            Text("$count items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (hasChildren || hasCachedChildren) {
                    Icon(
                        imageVector = if (isExpanded.value) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { checked -> onToggle(category.id, checked) }
                )
            }
            if (isExpanded.value && (hasChildren || hasCachedChildren)) {
                Column(modifier = Modifier.padding(start = 48.dp)) {
                    val children = if (hasCachedChildren) childLabels else category.children ?: emptyList()
                    children.forEach { child ->
                        val childSelected = child.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                child.icon?.let {
                                    Image(
                                        painter = rememberAsyncImagePainter(it),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 8.dp))
                                Text(child.name, style = MaterialTheme.typography.bodyMedium)
                            }
                            Checkbox(
                                checked = childSelected,
                                onCheckedChange = { checked -> onToggle(child.id, checked) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CookieDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onLogout: () -> Unit,
    hasCookie: Boolean
) {
    var cookieText by androidx.compose.runtime.mutableStateOf("")
    val errorMessage = androidx.compose.runtime.mutableStateOf<String?>(null)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cookie Settings") },
        text = {
            Column(modifier = Modifier.padding(16.dp).width(400.dp)) {
                if (hasCookie) {
                    Text("Current cookie is valid. You can update it or logout.", style = MaterialTheme.typography.bodyMedium)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                    Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text("Logout")
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                    Divider()
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                }
                Text("Paste your HoYoLAB cookie here (Netscape format or JSON):", style = MaterialTheme.typography.bodyMedium)
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                androidx.compose.material3.TextField(
                    value = cookieText,
                    onValueChange = { cookieText = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    label = { Text("Cookie") },
                    singleLine = false,
                    keyboardOptions = KeyboardOptions.Default,
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
                errorMessage.value?.let { msg ->
                    Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    if (cookieText.isNotBlank()) {
                        onSave(cookieText)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error", color = MaterialTheme.colorScheme.error) },
        text = {
            Text(message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}