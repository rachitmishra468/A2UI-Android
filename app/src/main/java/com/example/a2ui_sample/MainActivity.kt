package com.example.a2ui_sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.a2ui_sample.presentation.viewmodel.RestaurantViewModel
import com.example.a2ui_sample.presentation.screens.AiRestaurantScreen
import com.example.a2ui_sample.presentation.screens.HomeScreen
import com.example.a2ui_sample.presentation.screens.CartScreen
import com.example.a2ui_sample.presentation.screens.OrdersScreen
import com.example.a2ui_sample.presentation.screens.TableBookingScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.a2ui_sample.presentation.theme.A2UI_SampleTheme
import com.example.a2ui_sample.presentation.theme.RestaurantPrimary
import com.example.a2ui_sample.presentation.theme.RestaurantSecondary

/**
 * Bottom navigation destination. Every destination is reachable BOTH manually (tapping the tab)
 * and via AI agent prompts on the "ai" screen, since both paths share the same use cases
 * (MenuRepository, cart, table booking, and order use-cases).
 */
private data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem("home", "Menu", Icons.Default.Home),
    BottomNavItem("ai", "AI Chat", Icons.Default.SmartToy),
    BottomNavItem("cart", "Cart", Icons.Default.ShoppingCart),
    BottomNavItem("orders", "Orders", Icons.Default.Receipt),
    BottomNavItem("bookings", "Booking", Icons.Default.EventSeat)
)

/**
 * File: MainActivity.kt
 * Purpose: Entry point of the application. Sets up theme, navigation graph, and a persistent
 * bottom navigation bar covering Menu, AI Chat, Cart, Orders (current/past) and Table Booking.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            A2UI_SampleTheme {
                // Single shared ViewModel instance for the whole app (scoped to this Activity),
                // so chat history, cart, bookings and orders stay in sync and persist across
                // bottom-nav tab switches instead of each screen creating its own instance.
                val viewModel: RestaurantViewModel = viewModel()
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination

                Scaffold(
                    // IMPORTANT: exclude the IME (keyboard) inset from the Scaffold's own padding.
                    // Otherwise NavHost's innerPadding already reserves the keyboard height, and any
                    // screen (e.g. AiRestaurantScreen) that also calls imePadding() would double it,
                    // causing the input bar to "jump" too high with a white gap above the keyboard.
                    // Individual screens are responsible for handling their own imePadding() exactly once.

                    bottomBar = {
                        NavigationBar(containerColor = RestaurantPrimary) {
                            bottomNavItems.forEach { item ->
                                val selected = currentRoute?.hierarchy?.any { it.route == item.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = RestaurantSecondary,
                                        selectedTextColor = RestaurantSecondary,
                                        unselectedIconColor = Color.White.copy(alpha = 0.7f),
                                        unselectedTextColor = Color.White.copy(alpha = 0.7f),
                                        indicatorColor = Color.White.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") { HomeScreen(navController, viewModel) }
                        composable("ai") { AiRestaurantScreen(navController, viewModel) }
                        composable("cart") { CartScreen(navController, viewModel) }
                        composable("orders") { OrdersScreen(navController, viewModel) }
                        composable("bookings") { TableBookingScreen(navController, viewModel) }
                    }
                }
            }
        }
    }
}
