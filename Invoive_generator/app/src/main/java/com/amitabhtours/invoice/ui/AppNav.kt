package com.amitabhtours.invoice.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.amitabhtours.invoice.App
import com.amitabhtours.invoice.ui.customers.CustomerDetailScreen
import com.amitabhtours.invoice.ui.customers.CustomersScreen
import com.amitabhtours.invoice.ui.earnings.EarningsScreen
import com.amitabhtours.invoice.ui.history.HistoryScreen
import com.amitabhtours.invoice.ui.home.HomeScreen
import com.amitabhtours.invoice.ui.invoice.InvoiceDetailScreen
import com.amitabhtours.invoice.ui.invoice.NewInvoiceScreen
import com.amitabhtours.invoice.ui.more.AboutScreen
import com.amitabhtours.invoice.ui.more.HelpScreen
import com.amitabhtours.invoice.ui.more.MoreScreen
import com.amitabhtours.invoice.ui.more.OnboardingScreen
import com.amitabhtours.invoice.ui.more.SettingsScreen
import com.amitabhtours.invoice.ui.theme.AppTheme
import com.amitabhtours.invoice.ui.trips.TripDetailScreen
import com.amitabhtours.invoice.ui.trips.TripsScreen

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val EARNINGS = "earnings"
    const val CUSTOMERS = "customers"
    const val MORE = "more"
    const val NEW_INVOICE = "invoice/new?tripId={tripId}&customerId={customerId}"
    const val INVOICE = "invoice/{id}"
    const val CUSTOMER = "customer/{id}"
    const val TRIP = "trip/{id}"
    const val TRIPS = "trips"
    const val SETTINGS = "settings"
    const val HELP = "help?topic={topic}"
    const val ABOUT = "about"

    fun newInvoice(tripId: Long? = null, customerId: Long? = null) = "invoice/new?tripId=${tripId ?: -1}&customerId=${customerId ?: -1}"
    fun invoice(id: Long) = "invoice/$id"
    fun customer(id: Long) = "customer/$id"
    fun trip(id: Long) = "trip/$id"
    fun help(topic: String? = null) = if (topic == null) "help" else "help?topic=$topic"
}

/** ViewModel with access to the app's database/settings, without a DI framework. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (App) -> VM): VM {
    val app = LocalContext.current.applicationContext as App
    return viewModel(key = key, factory = viewModelFactory { initializer { create(app) } })
}

@Composable
fun app(): App = LocalContext.current.applicationContext as App

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Rounded.Home),
    Tab(Routes.HISTORY, "History", Icons.AutoMirrored.Rounded.ReceiptLong),
    Tab(Routes.EARNINGS, "Earnings", Icons.Rounded.BarChart),
    Tab(Routes.CUSTOMERS, "Customers", Icons.Rounded.Groups),
    Tab(Routes.MORE, "More", Icons.Rounded.MoreHoriz),
)

@Composable
fun AppRoot() {
    val app = app()
    val settings by app.settings.flow.collectAsState(initial = null)
    val s = settings
    AppTheme(darkMode = s?.darkMode ?: "system") {
        when {
            s == null -> Box(Modifier.fillMaxSize())
            !s.onboardingDone -> OnboardingScreen()
            else -> MainShell()
        }
    }
}

@Composable
private fun MainShell() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = tabs.any { it.route == route }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.goTab(tab.route) },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(inner).consumeWindowInsets(inner),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            composable(Routes.HOME) { HomeScreen(nav) }
            composable(Routes.HISTORY) { HistoryScreen(nav) }
            composable(Routes.EARNINGS) { EarningsScreen(nav) }
            composable(Routes.CUSTOMERS) { CustomersScreen(nav) }
            composable(Routes.MORE) { MoreScreen(nav) }
            composable(
                Routes.NEW_INVOICE,
                arguments = listOf(
                    navArgument("tripId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("customerId") { type = NavType.LongType; defaultValue = -1L },
                ),
                enterTransition = { slideInHorizontally { it / 3 } + fadeIn() },
                popExitTransition = { slideOutHorizontally { it / 3 } + fadeOut() },
            ) { e ->
                val trip = e.arguments?.getLong("tripId")?.takeIf { it > 0 }
                val customer = e.arguments?.getLong("customerId")?.takeIf { it > 0 }
                NewInvoiceScreen(nav, trip, customer)
            }
            composable(Routes.INVOICE, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                InvoiceDetailScreen(nav, e.arguments!!.getLong("id"))
            }
            composable(Routes.CUSTOMER, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                CustomerDetailScreen(nav, e.arguments!!.getLong("id"))
            }
            composable(Routes.TRIP, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                TripDetailScreen(nav, e.arguments!!.getLong("id"))
            }
            composable(Routes.TRIPS) { TripsScreen(nav) }
            composable(Routes.SETTINGS) { SettingsScreen(nav) }
            composable(Routes.HELP, arguments = listOf(navArgument("topic") { type = NavType.StringType; nullable = true; defaultValue = null })) { e ->
                HelpScreen(nav, e.arguments?.getString("topic"))
            }
            composable(Routes.ABOUT) { AboutScreen(nav) }
        }
    }
}

fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
