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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.a2ui_sample.domain.model.Order
import com.example.a2ui_sample.presentation.viewmodel.RestaurantMainViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBack: () -> Unit,
    viewModel: RestaurantMainViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val orders by viewModel.allOrders.collectAsState(initial = emptyList())
    
    val filteredOrders = orders.filter { order ->
        val matchesSearch = order.id.value.contains(searchQuery, ignoreCase = true)
        val matchesFilter = if (selectedFilter == "All") true else order.status.name.equals(selectedFilter, ignoreCase = true)
        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF8F8F8))) {
            // Search and Filter
            OrderHistoryControls(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                selectedFilter = selectedFilter,
                onFilterChange = { selectedFilter = it }
            )

            if (filteredOrders.isEmpty()) {
                EmptyOrdersView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredOrders) { order ->
                        OrderHistoryCard(order, onReorder = { 
                            order.items.forEach { viewModel.addToCart(it.menuItemId) }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun OrderHistoryControls(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: String,
    onFilterChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by Order ID") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val filters = listOf("All", "DELIVERED", "PENDING", "CANCELLED")
            filters.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter) }
                )
            }
        }
    }
}

@Composable
fun OrderHistoryCard(order: Order, onReorder: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.id.value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(order.id.value))
                        Toast.makeText(context, "Order ID Copied Successfully", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy, 
                            contentDescription = "Copy ID", 
                            modifier = Modifier.size(16.dp), 
                            tint = Color.Gray
                        )
                    }
                }
                Text(order.status.name, color = getStatusColor(order.status.name), fontWeight = FontWeight.Medium)
            }
            Text("Placed on: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(order.orderTime))}", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            order.items.forEach { item ->
                Text("${item.quantity}x ${item.menuItemName}", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Total: ₹${order.totalAmount.amount}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Button(onClick = onReorder, shape = RoundedCornerShape(8.dp)) {
                    Text("Reorder")
                }
            }
        }
    }
}

@Composable
fun EmptyOrdersView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No orders found", fontSize = 18.sp, color = Color.Gray)
    }
}

fun getStatusColor(status: String): Color {
    return when(status.uppercase()) {
        "DELIVERED", "COMPLETED", "CONFIRMED" -> Color(0xFF4CAF50)
        "PENDING" -> Color(0xFFFF9800)
        "CANCELLED" -> Color(0xFFF44336)
        else -> Color.Gray
    }
}
