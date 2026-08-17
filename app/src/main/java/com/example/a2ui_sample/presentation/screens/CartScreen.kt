package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.a2ui_sample.domain.model.CartItem
import com.example.a2ui_sample.presentation.viewmodel.RestaurantMainViewModel
import com.example.a2ui_sample.presentation.theme.PremiumColors
import com.example.a2ui_sample.presentation.components.PremiumCard
import com.example.a2ui_sample.presentation.components.PremiumButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    onNavigateToOrderHistory: () -> Unit,
    viewModel: RestaurantMainViewModel = hiltViewModel()
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val subtotal = cartItems.sumOf { it.menuItem.price.amount * it.quantity }
    val tax = (subtotal * 0.05).toInt()
    val delivery = if (subtotal > 0) 40 else 0
    val total = subtotal + tax + delivery

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToOrderHistory) {
                        Icon(Icons.Outlined.History, contentDescription = "Order History")
                    }
                }
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                PremiumCartBottomBar(total, onCheckout)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (cartItems.isEmpty()) {
                PremiumEmptyCartView(onBack)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "Order Summary",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(cartItems) { item ->
                        PremiumCartItemRow(
                            item = item,
                            onIncrease = { viewModel.updateCartQuantity(item.menuItem.id, item.quantity + 1) },
                            onDecrease = { viewModel.updateCartQuantity(item.menuItem.id, item.quantity - 1) },
                            onRemove = { viewModel.removeFromCart(item.menuItem.id) }
                        )
                    }

                    item {
                        PremiumOrderDetails(subtotal, tax, delivery, total)
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumEmptyCartView(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = PremiumColors.Gray100
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ShoppingCart, contentDescription = null, modifier = Modifier.size(48.dp), tint = PremiumColors.Gray400)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Your cart is empty", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Looks like you haven't added anything to your cart yet.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = PremiumColors.Gray500
        )
        Spacer(modifier = Modifier.height(32.dp))
        PremiumButton(text = "Start Ordering", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun PremiumCartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = item.menuItem.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.menuItem.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("₹${item.menuItem.price.amount}", color = PremiumColors.Gray500, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(PremiumColors.Gray100, RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = onDecrease, modifier = Modifier.size(24.dp)) { 
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp)) 
                    }
                    Text(
                        item.quantity.toString(), 
                        modifier = Modifier.padding(horizontal = 12.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    IconButton(onClick = onIncrease, modifier = Modifier.size(24.dp)) { 
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) 
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = PremiumColors.Error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun PremiumOrderDetails(subtotal: Int, tax: Int, delivery: Int, total: Int) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Text("Payment Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        DetailRow("Subtotal", "₹$subtotal")
        DetailRow("Service Tax (5%)", "₹$tax")
        DetailRow("Delivery Fee", "₹$delivery")
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = PremiumColors.Gray100)
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Amount", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("₹$total", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PremiumColors.Accent)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = PremiumColors.Gray500, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
fun PremiumCartBottomBar(total: Int, onCheckout: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 16.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(0.4f)) {
                Text("Total", fontSize = 12.sp, color = PremiumColors.Gray500)
                Text("₹$total", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PremiumColors.Accent)
            }
            PremiumButton(
                text = "Place Order",
                onClick = onCheckout,
                modifier = Modifier.weight(0.6f),
                containerColor = PremiumColors.Primary
            )
        }
    }
}
