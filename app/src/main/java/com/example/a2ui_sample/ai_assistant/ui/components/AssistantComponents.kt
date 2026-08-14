package com.example.a2ui_sample.ai_assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.a2ui_sample.ai_assistant.viewmodel.AssistantViewModel
import com.example.a2ui_sample.domain.model.MenuItem

@Composable
fun MenuItemCard(
    item: MenuItem,
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val cartItem = viewModel.cartItems.find { it.menuItem.id == item.id }
    val quantity = cartItem?.quantity ?: 0

    Card(
        modifier = modifier
            .width(200.dp)
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                model = item.image,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Row(
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${item.price?.amount ?: 0}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (quantity > 0) {
                        QuantitySelector(
                            quantity = quantity,
                            onIncrement = { viewModel.updateCartQuantity(item.id, quantity + 1) },
                            onDecrement = { viewModel.updateCartQuantity(item.id, quantity - 1) }
                        )
                    } else {
                        IconButton(
                            onClick = { viewModel.addToCart(item.id) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add to cart",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuantitySelector(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier.width(80.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onDecrement,
            modifier = Modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
        }
        Text(
            text = quantity.toString(), 
            style = MaterialTheme.typography.bodyMedium, 
            fontWeight = FontWeight.Bold
        )
        IconButton(
            onClick = onIncrement,
            modifier = Modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun MenuHorizontalList(items: List<MenuItem>, viewModel: AssistantViewModel) {
    android.util.Log.d("AssistantFlow", "🎬 Rendering MenuHorizontalList with ${items.size} items")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            MenuItemCard(item = item, viewModel = viewModel)
        }
    }
}

@Composable
fun MenuDetailCard(item: MenuItem, viewModel: AssistantViewModel) {
    android.util.Log.d("AssistantFlow", "🎬 Rendering MenuDetailCard for ${item.name}")
    val cartItem = viewModel.cartItems.find { it.menuItem.id == item.id }
    val quantity = cartItem?.quantity ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = item.image,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = item.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Price: ₹${item.price?.amount ?: 0}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                
                if (quantity > 0) {
                    QuantitySelector(
                        quantity = quantity,
                        onIncrement = { viewModel.updateCartQuantity(item.id, quantity + 1) },
                        onDecrement = { viewModel.updateCartQuantity(item.id, quantity - 1) }
                    )
                } else {
                    Button(
                        onClick = { viewModel.addToCart(item.id) },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add to Cart")
                    }
                }
            }
        }
    }
}

@Composable
fun CartUpdateCard(item: MenuItem, quantity: Int, message: String) {
    android.util.Log.d("AssistantFlow", "🎬 Rendering CartUpdateCard: item=${item.name}, qty=$quantity")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.image,
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                val displayText = if (message.isBlank()) "Added ${item.name}" else message
                Text(text = displayText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (quantity > 0) {
                    Text(text = "Qty: $quantity", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun CartCard(items: List<com.example.a2ui_sample.domain.model.CartItem>, total: Int, message: String) {
    android.util.Log.d("AssistantFlow", "🎬 Rendering CartCard with ${items.size} items, total: $total")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "🛒 Shopping Cart", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (items.isEmpty()) {
                // message should already say it's empty
            } else {
                items.forEach { cartItem ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = cartItem.menuItem.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(text = "${cartItem.quantity} x ₹${cartItem.menuItem.price.amount}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(text = "₹${cartItem.menuItem.price.amount * cartItem.quantity}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "₹$total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun BookingCard(message: String, date: String?, time: String?, guests: Int?) {
    android.util.Log.d("AssistantFlow", "🎬 Rendering BookingCard: $message")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "📅 Table Booking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
            if (date != null || time != null || guests != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                if (date != null) Text(text = "Date: $date", style = MaterialTheme.typography.bodyMedium)
                if (time != null) Text(text = "Time: $time", style = MaterialTheme.typography.bodyMedium)
                if (guests != null) Text(text = "Guests: $guests", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun FeedbackCard(message: String, rating: Int?) {
    android.util.Log.d("AssistantFlow", "🎬 Rendering FeedbackCard: rating=$rating")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "⭐ Feedback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
            if (rating != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    repeat(5) { index ->
                        Text(
                            text = "★",
                            color = if (index < rating) Color(0xFFFFD700) else Color.LightGray,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderStatusCard(message: String, status: String?, eta: String?) {
    android.util.Log.d("AssistantFlow", "🎬 Rendering OrderStatusCard: status=$status, eta=$eta")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "🚚 Order Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
            if (status != null || eta != null) {
                Spacer(modifier = Modifier.height(8.dp))
                if (status != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = status.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                if (eta != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Estimated Arrival: $eta", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun CheckoutCard(
    items: List<com.example.a2ui_sample.domain.model.CartItem>,
    total: Int,
    message: String,
    viewModel: AssistantViewModel
) {
    android.util.Log.d("AssistantFlow", "🎬 Rendering CheckoutCard with ${items.size} items, total: $total")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "🏁 Order Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))
            
            items.forEach { cartItem ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "${cartItem.quantity} x ${cartItem.menuItem.name}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "₹${cartItem.menuItem.price.amount * cartItem.quantity}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Total Amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "₹$total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.placeOrder(isCod = false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("💳 Payment")
                }
                OutlinedButton(
                    onClick = { viewModel.placeOrder(isCod = true) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("💵 COD")
                }
            }
        }
    }
}
