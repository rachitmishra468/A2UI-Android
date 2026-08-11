package com.example.a2ui_sample.presentation.components

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.a2ui_sample.presentation.screens.*
import com.example.a2ui_sample.ai_assistant.ui.AssistantChatScreen

/**
 * AppNavigation
 * Centralized navigation for the enterprise application.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToChat = { navController.navigate("chat") },
                onNavigateToAssistant = { navController.navigate("ai_assistant") },
                onNavigateToMenu = { navController.navigate("menu") },
                onNavigateToReservations = { navController.navigate("reservation") },
                onNavigateToCart = { navController.navigate("cart") }
            )
        }
        composable("ai_assistant") {
            AssistantChatScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCart = { navController.navigate("cart") }
            )
        }
        composable("chat") {
            ChatScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCart = { navController.navigate("cart") },
                onNavigateToCheckout = { navController.navigate("checkout") },
                onNavigateToBookings = { navController.navigate("booking_history") },
                onNavigateToOrders = { navController.navigate("orders") },
                onNavigateToMenu = { navController.navigate("menu") }
            )
        }
        composable("menu") {
            MenuScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCart = { navController.navigate("cart") }
            )
        }
        composable("cart") {
            CartScreen(
                onBack = { navController.popBackStack() },
                onCheckout = { navController.navigate("checkout") },
                onNavigateToOrderHistory = { navController.navigate("orders") }
            )
        }
        composable("checkout") {
            CheckoutScreen(
                onBack = { navController.popBackStack() },
                onOrderPlaced = { orderId ->
                    navController.navigate("order_confirmation/$orderId") {
                        popUpTo("cart") { inclusive = true }
                    }
                }
            )
        }
        composable(
            "order_confirmation/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderConfirmationScreen(
                orderId = orderId,
                onContinue = { navController.navigate("home") { popUpTo("home") { inclusive = true } } }
            )
        }
        composable("reservation") {
            BookingScreen(
                onBack = { navController.popBackStack() },
                onBookingConfirmed = {
                    navController.navigate("booking_confirmation")
                },
                onViewHistory = {
                    navController.navigate("booking_history")
                }
            )
        }
        composable("booking_confirmation") {
            BookingConfirmationScreen(
                onViewHistory = {
                    navController.navigate("booking_history") {
                        popUpTo("reservation") { inclusive = true }
                    }
                },
                onBackHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
        composable("booking_history") {
            BookingHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("orders") {
            OrderHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
