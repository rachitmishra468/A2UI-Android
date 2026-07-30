@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.a2ui_sample.domain.model.Order
import com.example.a2ui_sample.domain.model.OrderStatus
import com.example.a2ui_sample.presentation.viewmodel.RestaurantViewModel
import com.example.a2ui_sample.presentation.theme.*

/**
 * OrdersScreen
 * Shows "Current Orders" (preparing) and "Past Orders" (completed) as two tabs.
 * Orders are created both by manual checkout (CartScreen) and by agent prompts
 * (e.g. "checkout") since both paths go through the same CheckoutUseCase.
 */
@Composable
fun OrdersScreen(navController: NavController? = null, viewModel: RestaurantViewModel = viewModel()) {
    // Recompose when orders change
    val updateTrigger = viewModel.cartUpdateTrigger.value
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = RestaurantPrimary,
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "📦 My Orders",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        TabRow(selectedTabIndex = selectedTab, containerColor = Color.White) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Current Order") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Past Order") }
            )
        }

        val orders = if (selectedTab == 0) viewModel.getCurrentOrders() else viewModel.getPastOrders()

        if (orders.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(if (selectedTab == 0) "🍳" else "🧾", fontSize = 48.sp)
                Text(
                    if (selectedTab == 0) "No active orders" else "No past orders yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    if (selectedTab == 0) "Add items to cart and checkout, or tell the AI \"checkout\"" else "Completed orders will show up here",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders) { order ->
                    OrderCard(
                        order = order,
                        showCompleteButton = selectedTab == 0,
                        onComplete = { viewModel.completeOrder(order.orderId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order, showCompleteButton: Boolean, onComplete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Order ${order.orderId}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                val statusColor = if (order.status == OrderStatus.PREPARING) RestaurantAccent else RestaurantSuccess
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (order.status == OrderStatus.PREPARING) "Preparing" else "Delivered",
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            order.items.forEach { item ->
                Text(
                    "${item.menuItem.name} x ${item.quantity} = ₹${item.menuItem.price * item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = BorderGray, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total: ₹${order.totalAmount}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = RestaurantSecondary
                )
                if (showCompleteButton) {
                    Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = RestaurantPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Mark Delivered", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
