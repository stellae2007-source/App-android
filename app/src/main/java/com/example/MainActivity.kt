package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ConnectionState
import com.example.service.ObdMonitoringService
import com.example.ui.MainViewModel
import com.example.ui.connection.ConnectionScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.debug.DebugScreen
import com.example.ui.diagnostics.DiagnosticsScreen
import com.example.ui.driving.DrivingModeScreen
import com.example.ui.graphs.GraphsScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.navigation.MainDestination
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.ClusterBackground
import com.example.ui.theme.ClusterCardBorder
import com.example.ui.theme.ClusterSurface
import com.example.ui.theme.GaugeCyan
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeOrange
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusScanning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.vehicles.VehiclesScreen
import com.example.utils.GpsTracker

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var gpsTracker: GpsTracker? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize GPS tracker to compare OBD speed with GPS and track trip route
        gpsTracker = GpsTracker(this) { speedKmh, lat, lon, alt, distKm ->
            viewModel.updateGpsTelemetry(speedKmh, lat, lon, alt, distKm)
        }

        setContent {
            MyApplicationTheme {
                RequestNeededPermissions()

                // Start / Stop foreground service alongside connection state
                val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
                LaunchedEffect(connectionState) {
                    if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.MONITORING) {
                        ObdMonitoringService.start(this@MainActivity)
                        gpsTracker?.startTracking()
                    } else if (connectionState == ConnectionState.DISCONNECTED) {
                        ObdMonitoringService.stop(this@MainActivity)
                        gpsTracker?.stopTracking()
                    }
                }

                MainScreenContent(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gpsTracker?.stopTracking()
    }
}

@Composable
fun RequestNeededPermissions() {
    val context = LocalContext.current
    val permissionsToRequest = remember {
        mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permissions granted callback
    }

    LaunchedEffect(Unit) {
        val notGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            launcher.launch(notGranted.toTypedArray())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(viewModel: MainViewModel) {
    var currentDestination by remember { mutableStateOf(MainDestination.DASHBOARD) }
    var menuExpanded by remember { mutableStateOf(false) }

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val activeAlerts by viewModel.activeAlerts.collectAsStateWithLifecycle()
    val dtcs by viewModel.storedDtcs.collectAsStateWithLifecycle()

    val statusDotColor = when (connectionState) {
        ConnectionState.MONITORING, ConnectionState.CONNECTED -> StatusConnected
        ConnectionState.SCANNING, ConnectionState.CONNECTING, ConnectionState.INITIALIZING -> StatusScanning
        ConnectionState.RECONNECTING -> GaugeOrange
        ConnectionState.ERROR -> StatusError
        ConnectionState.DISCONNECTED, ConnectionState.READY -> StatusDisconnected
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ClusterBackground)) {
        val isExpandedScreen = maxWidth >= 700.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Adaptive Navigation Rail for Tablets / Wide Screens
            if (isExpandedScreen) {
                NavigationRail(
                    containerColor = ClusterSurface,
                    contentColor = TextPrimary,
                    modifier = Modifier.fillMaxHeight().border(1.dp, ClusterCardBorder)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GaugeCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = GaugeCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    MainDestination.values().forEach { destination ->
                        NavigationRailItem(
                            selected = currentDestination == destination,
                            onClick = { currentDestination = destination },
                            icon = {
                                if (destination == MainDestination.DIAGNOSTICS && dtcs.isNotEmpty()) {
                                    BadgedBox(badge = { Badge(containerColor = GaugeRed) { Text("${dtcs.size}") } }) {
                                        Icon(destination.icon, contentDescription = destination.title)
                                    }
                                } else {
                                    Icon(destination.icon, contentDescription = destination.title)
                                }
                            },
                            label = { Text(destination.title, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = GaugeCyan,
                                indicatorColor = GaugeCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            }

            // Main Content Area
            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = ClusterBackground,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(statusDotColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentDestination.title.uppercase(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = TextPrimary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = ClusterSurface,
                            titleContentColor = TextPrimary
                        ),
                        actions = {
                            // Quick Jump Tabs for Secondary Features
                            if (!isExpandedScreen) {
                                IconButton(onClick = { currentDestination = MainDestination.GRAPHS }) {
                                    Icon(
                                        Icons.Default.Insights,
                                        contentDescription = "Gráficos",
                                        tint = if (currentDestination == MainDestination.GRAPHS) GaugeCyan else TextSecondary
                                    )
                                }
                                IconButton(onClick = { currentDestination = MainDestination.DEBUG }) {
                                    Icon(
                                        Icons.Default.Terminal,
                                        contentDescription = "Terminal",
                                        tint = if (currentDestination == MainDestination.DEBUG) GaugeCyan else TextSecondary
                                    )
                                }
                            }

                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menú", tint = TextSecondary)
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(ClusterSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Gráficos en tiempo real", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Insights, contentDescription = null, tint = GaugeCyan) },
                                    onClick = {
                                        currentDestination = MainDestination.GRAPHS
                                        menuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Perfiles de Vehículos", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = GaugeGreen) },
                                    onClick = {
                                        currentDestination = MainDestination.VEHICLES
                                        menuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Configuración OBD", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = GaugeOrange) },
                                    onClick = {
                                        currentDestination = MainDestination.SETTINGS
                                        menuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Terminal / Debug OBD", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null, tint = GaugeCyan) },
                                    onClick = {
                                        currentDestination = MainDestination.DEBUG
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    // Mobile Bottom Navigation Bar (hidden on expanded tablets which have NavigationRail)
                    if (!isExpandedScreen) {
                        NavigationBar(
                            containerColor = ClusterSurface,
                            contentColor = TextPrimary,
                            modifier = Modifier.border(1.dp, ClusterCardBorder)
                        ) {
                            val bottomNavItems = MainDestination.values().filter { it.isPrimaryBottomNav }
                            bottomNavItems.forEach { destination ->
                                val isSelected = currentDestination == destination
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentDestination = destination },
                                    icon = {
                                        if (destination == MainDestination.DIAGNOSTICS && dtcs.isNotEmpty()) {
                                            BadgedBox(badge = { Badge(containerColor = GaugeRed) { Text("${dtcs.size}") } }) {
                                                Icon(destination.icon, contentDescription = destination.title)
                                            }
                                        } else {
                                            Icon(destination.icon, contentDescription = destination.title)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = destination.title,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = GaugeCyan,
                                        indicatorColor = GaugeCyan,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentDestination) {
                        MainDestination.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                        MainDestination.DRIVING -> DrivingModeScreen(viewModel = viewModel)
                        MainDestination.CONNECTION -> ConnectionScreen(viewModel = viewModel)
                        MainDestination.DIAGNOSTICS -> DiagnosticsScreen(viewModel = viewModel)
                        MainDestination.HISTORY -> HistoryScreen(viewModel = viewModel)
                        MainDestination.GRAPHS -> GraphsScreen(viewModel = viewModel)
                        MainDestination.VEHICLES -> VehiclesScreen(viewModel = viewModel)
                        MainDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        MainDestination.DEBUG -> DebugScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
