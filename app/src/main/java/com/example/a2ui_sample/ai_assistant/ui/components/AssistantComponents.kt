package com.example.a2ui_sample.ai_assistant.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.a2ui_sample.ai_assistant.viewmodel.AssistantViewModel
import com.example.a2ui_sample.domain.model.MenuItem
import com.example.a2ui_sample.presentation.theme.PremiumColors

@Composable
fun CouponListCard(coupons: List<com.example.a2ui_sample.domain.model.Coupon>, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalActivity, contentDescription = null, tint = PremiumColors.Accent)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Available Coupons", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            coupons.forEach { coupon ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    color = PremiumColors.Accent.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PremiumColors.Accent.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(coupon.code, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = PremiumColors.Accent)
                            Text(coupon.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Text("${coupon.discountPercentage}% OFF", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }
            
            if (message.isNotBlank() && message != "Here are the available coupons.") {
                Spacer(modifier = Modifier.height(12.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
            }
        }
    }
}

@Composable
fun InfoDisplayCard(title: String, content: String, icon: String? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val vector = when(icon) {
                    "policy", "rule" -> Icons.Default.Gavel
                    "privacy" -> Icons.Default.PrivacyTip
                    "info" -> Icons.Default.Info
                    else -> Icons.Default.MenuBook
                }
                Icon(vector, contentDescription = null, tint = PremiumColors.Primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                content, 
                style = MaterialTheme.typography.bodyMedium, 
                color = Color.DarkGray,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun MenuHorizontalList(items: List<MenuItem>, viewModel: AssistantViewModel) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        items(items) { item ->
            MenuSmallCard(item = item, onAdd = { viewModel.addToCart(item.id) })
        }
    }
}

@Composable
fun MenuSmallCard(item: MenuItem, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text(text = "₹${item.price.amount}", color = PremiumColors.Primary, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumColors.Accent)
                ) {
                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun OrderStatusCard(message: String, status: String?, eta: String?, progress: Float = 0f) {
    val steps = listOf("Confirmed", "Preparing", "Out for Delivery", "Delivered")
    val currentStep = when (status?.uppercase()) {
        "PENDING", "CONFIRMED" -> 0
        "PREPARING" -> 1
        "READY", "PICKED_UP" -> 2
        "DELIVERED", "COMPLETED" -> 3
        else -> 0
    }

    // Principle Engineer Fix: Align progress bar exactly with step dots (0, 0.33, 0.66, 1.0)
    // If progress is provided from outside, use it; otherwise, use step-based progress
    val targetProgress = if (progress > 0f) progress else currentStep / 3f

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "progress"
    )

    val colorTransition by animateColorAsState(
        targetValue = when (currentStep) {
            0 -> Color(0xFFFFC107) // Yellow - Pending/Confirmed
            1 -> Color(0xFFFF9800) // Orange - Preparing
            2 -> Color(0xFF2196F3) // Blue - Out for Delivery
            3 -> Color(0xFF4CAF50) // Green - Delivered
            else -> Color(0xFFFFC107)
        },
        animationSpec = tween(800),
        label = "color"
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(colorTransition.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentStep == 3) Icons.Default.CheckCircle else Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = colorTransition,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Live Tracking", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    val statusText = when (status?.uppercase()) {
                        "PENDING" -> "Order Placed"
                        "CONFIRMED" -> "Confirmed"
                        "PREPARING" -> "Preparing"
                        "READY", "PICKED_UP" -> "Out for Delivery"
                        "DELIVERED", "COMPLETED" -> "Delivered"
                        else -> status?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Processing"
                    }
                    Text(text = statusText, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = colorTransition)
                }
            }

            if (eta != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = PremiumColors.Primary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = PremiumColors.Primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Estimated Arrival: $eta", style = MaterialTheme.typography.bodySmall, color = PremiumColors.Primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Visual Progress Dots
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background Track
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.Center).clip(CircleShape).background(Color(0xFFF0F0F0)))
                
                // Active Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(4.dp)
                        .align(Alignment.CenterStart)
                        .clip(CircleShape)
                        .background(colorTransition)
                )

                // Dots at 0, 33, 66, 100
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(if (i <= currentStep) colorTransition else Color(0xFFE0E0E0), CircleShape)
                                .align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                steps.forEachIndexed { index, step ->
                    Text(
                        text = step,
                        fontSize = 9.sp,
                        fontWeight = if (index == currentStep) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (index <= currentStep) colorTransition else Color.Gray,
                        modifier = Modifier.width(64.dp),
                        textAlign = TextAlign.Center,
                        lineHeight = 10.sp
                    )
                }
            }

            if (message.isNotBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    color = Color(0xFFF9F9F9),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CartUpdateCard(item: MenuItem, quantity: Int, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = PremiumColors.Gray50),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = PremiumColors.Primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = message, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = "${item.name} x $quantity", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun CartCard(items: List<com.example.a2ui_sample.domain.model.CartItem>, total: Int, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "🛒 Your Cart", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            items.forEach { 
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${it.quantity}x ${it.menuItem.name}", fontSize = 14.sp)
                    Text("₹${it.menuItem.price.amount * it.quantity}", fontWeight = FontWeight.Medium)
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontWeight = FontWeight.Bold)
                Text("₹$total", fontWeight = FontWeight.ExtraBold, color = PremiumColors.Primary)
            }
        }
    }
}

@Composable
fun CheckoutCard(items: List<com.example.a2ui_sample.domain.model.CartItem>, total: Int, message: String, viewModel: AssistantViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PremiumColors.Primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Order Summary", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            Text("Final Step", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            items.take(3).forEach { 
                Text("• ${it.quantity}x ${it.menuItem.name}", color = Color.White, fontSize = 14.sp)
            }
            if (items.size > 3) Text("...and ${items.size - 3} more", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)

            Spacer(modifier = Modifier.height(20.dp))
            
            Text("Total: ₹$total", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.placeOrder(isCod = true) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("COD", fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { viewModel.navigateToCheckout() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PremiumColors.Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Pay Now", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BookingCard(message: String, date: String?, time: String?, guests: Int?) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EventAvailable, contentDescription = null, tint = Color(0xFF2E7D32))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Table Booked!", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message)
            if (date != null && time != null) {
                Text(text = "📅 $date at $time", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun FeedbackCard(message: String, rating: Int?) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = if ((rating ?: 0) >= 4) "🌟" else "📝", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = message, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun RatingRequestCard(orderId: String, message: String, viewModel: AssistantViewModel) {
    var selectedRating by remember { mutableStateOf(0) }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("How was your meal?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Order #$orderId", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { star ->
                    IconButton(onClick = { selectedRating = star }) {
                        Icon(
                            imageVector = if (star <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (star <= selectedRating) Color(0xFFFFC107) else Color.LightGray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            if (selectedRating > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.submitFeedback(orderId, selectedRating, "Great food!") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Submit Rating")
                }
            }
        }
    }
}

@Composable
fun MenuDetailCard(item: MenuItem, viewModel: AssistantViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.name, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    Text("₹${item.price.amount}", color = PremiumColors.Primary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.description, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.addToCart(item.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumColors.Accent)
                ) {
                    Text("Add to Cart", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
