package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.a2ui_sample.domain.model.Order
import com.example.a2ui_sample.domain.valueobjects.OrderStatus
import com.example.a2ui_sample.presentation.viewmodel.RestaurantMainViewModel
import org.a2ui.compose.animation.AnimatedCard
import org.a2ui.compose.animation.AnimatedListItem
import org.a2ui.compose.animation.AnimatedText
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBack: () -> Unit,
    viewModel: RestaurantMainViewModel = hiltViewModel()
) {
    val orders = viewModel.getPastOrders() + viewModel.getCurrentOrders()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Orders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (orders.isEmpty()) {
            EmptyState("No orders yet", Icons.Default.ReceiptLong, padding)
        } else {
            val sortedOrders = orders.sortedByDescending { it.orderTime }
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFFF8F8F8)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(sortedOrders.size) { index ->
                    val order = sortedOrders[index]
                    AnimatedListItem(index = index) {
                        OrderCard(order)
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(order: Order) {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(order.orderTime))

    AnimatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AnimatedText(order.id.value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                OrderStatusBadge(order.status)
            }
            Text(dateString, fontSize = 12.sp, color = Color.Gray)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            
            order.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${item.quantity}x ${item.menuItemName}", fontSize = 14.sp)
                    Text("₹${item.unitPrice.amount * item.quantity}", fontSize = 14.sp)
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Amount", fontWeight = FontWeight.SemiBold)
                AnimatedText("₹${order.totalAmount.amount}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
            }
        }
    }
}

@Composable
fun OrderStatusBadge(status: OrderStatus) {
    val color = when(status) {
        OrderStatus.COMPLETED, OrderStatus.DELIVERED -> Color(0xFF4CAF50)
        OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING -> Color(0xFF2196F3)
        OrderStatus.CANCELLED -> Color(0xFFF44336)
        else -> Color.Gray
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            status.name,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
