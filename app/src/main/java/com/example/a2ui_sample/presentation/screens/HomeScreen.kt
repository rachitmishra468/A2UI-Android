package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.a2ui_sample.presentation.a2ui.MenuA2UISection
import com.example.a2ui_sample.presentation.viewmodel.RestaurantMainViewModel
import org.a2ui.compose.animation.AnimatedCard
import org.a2ui.compose.animation.AnimatedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToReservations: () -> Unit,
    onNavigateToCart: () -> Unit,
    viewModel: RestaurantMainViewModel = hiltViewModel()
) {
    val cartItems = viewModel.getCartItems()
    val cartCount = cartItems.sumOf { it.quantity }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToChat,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                text = { Text("Ask AI Assistant") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF8F8F8))
        ) {
            // 1. Hero Banner (Compose)
            HeroBanner()

            // 2. Quick Actions (Compose)
            QuickActionsSection(onNavigateToMenu, onNavigateToReservations, onNavigateToCart, cartCount)

            // 3. Featured Items (A2UI)
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Chef's Specials 👨‍🍳", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                MenuA2UISection(viewModel)
            }

            // 4. Offer Banner (Compose)
            OfferBanner()

            Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB
        }
    }
}

@Composable
fun HeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        AsyncImage(
            model = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?q=80&w=1000&h=600&fit=crop",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                "Luxe Dining",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Experience the finest flavors",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun QuickActionsSection(
    onMenu: () -> Unit,
    onReservations: () -> Unit,
    onCart: () -> Unit,
    cartCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionCard("Menu", Icons.Default.RestaurantMenu, Color(0xFFFFE0B2), onMenu, Modifier.weight(1f))
        ActionCard("Booking", Icons.Default.Event, Color(0xFFC8E6C9), onReservations, Modifier.weight(1f))
        ActionCard("Cart", Icons.Default.ShoppingCart, Color(0xFFD1E3FF), onCart, Modifier.weight(1f), cartCount)
    }
}

@Composable
fun ActionCard(
    title: String, 
    icon: ImageVector, 
    color: Color, 
    onClick: () -> Unit, 
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    Box(modifier = modifier) {
        AnimatedCard(
            modifier = Modifier.height(100.dp).fillMaxWidth(),
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onClick() }
                        .background(color), 
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon, contentDescription = null, tint = Color.Black.copy(alpha = 0.7f), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        AnimatedText(
                            title, 
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold, 
                                color = Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }
        )
        if (badgeCount > 0) {
            Badge(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                containerColor = Color.Red,
                contentColor = Color.White
            ) {
                Text(badgeCount.toString())
            }
        }
    }
}

@Composable
fun OfferBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFF9C4)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("50% OFF on first order!", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("Use code: WELCOME50", color = Color.Gray)
            }
        }
    }
}
