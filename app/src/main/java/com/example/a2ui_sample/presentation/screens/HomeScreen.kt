package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.a2ui_sample.presentation.a2ui.MenuA2UISection
import com.example.a2ui_sample.presentation.viewmodel.RestaurantMainViewModel
import com.example.a2ui_sample.presentation.components.PremiumCard
import com.example.a2ui_sample.presentation.components.StatCard
import com.example.a2ui_sample.presentation.theme.PremiumColors
import com.example.a2ui_sample.presentation.theme.PremiumSpacing
import org.a2ui.compose.animation.AnimatedCard
import org.a2ui.compose.animation.AnimatedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToReservations: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToCoupons: () -> Unit,
    viewModel: RestaurantMainViewModel = hiltViewModel()
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val cartCount = cartItems.sumOf { it.quantity }
    val scrollState = rememberScrollState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToChat,
                containerColor = PremiumColors.Accent,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Premium Header
            HeaderSection()

            // 2. Modern Hero Banner
            ModernHeroSection()

            // 3. Quick Actions Grid
            QuickActionsGrid(
                onMenu = onNavigateToMenu,
                onReservations = onNavigateToReservations,
                onCart = onNavigateToCart,
                onAssistant = onNavigateToAssistant,
                onCoupons = onNavigateToCoupons,
                cartCount = cartCount
            )

            // 4. Today's Specials Section (A2UI)
            Column(modifier = Modifier.padding(PremiumSpacing.Medium)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Today's Specials",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onNavigateToMenu) {
                        Text("Explore", color = PremiumColors.Accent)
                    }
                }
                Spacer(modifier = Modifier.height(PremiumSpacing.Small))
                MenuA2UISection(viewModel)
            }

            // 6. Promotional Banner
            PremiumOfferBanner()

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "Good evening,",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                "Rachit Mishra",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(PremiumColors.Gray100),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = PremiumColors.Gray600)
        }
    }
}

@Composable
fun ModernHeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        AsyncImage(
            model = "https://images.unsplash.com/photo-1559339352-11d035aa65de?q=80&w=1000",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                    )
                )
        )

        // Glassmorphism Content Card
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .fillMaxWidth(0.7f),
            color = Color.White.copy(alpha = 0.15f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Monsoon Maharaja Feast",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Savor the rich flavors of Old Delhi's rainy season",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun QuickActionsGrid(
    onMenu: () -> Unit,
    onReservations: () -> Unit,
    onCart: () -> Unit,
    onAssistant: () -> Unit,
    onCoupons: () -> Unit,
    cartCount: Int
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp)
    ) {
        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            color = PremiumColors.Primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernActionCard("Menu", Icons.Outlined.Restaurant, PremiumColors.Gray50, onMenu, Modifier.weight(1f))
            ModernActionCard("Booking", Icons.Outlined.CalendarMonth, PremiumColors.Gray50, onReservations, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernActionCard("Cart", Icons.Outlined.LocalMall, PremiumColors.Gray50, onCart, Modifier.weight(1f), cartCount)
            ModernActionCard("Assistant", Icons.Outlined.SupportAgent, PremiumColors.Gray50, onAssistant, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernActionCard("Offers", Icons.Outlined.LocalOffer, PremiumColors.Gray50, onCoupons, Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f)) // Placeholder to keep grid balanced
        }
    }
}

@Composable
fun ModernActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    PremiumCard(
        modifier = modifier.clickable { onClick() },
        backgroundColor = color
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PremiumColors.Primary, modifier = Modifier.size(20.dp))
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(16.dp)
                            .background(PremiumColors.Accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(badgeCount.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
fun PremiumOfferBanner() {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PremiumColors.PremiumGradient)
    ) {
        // Decorative elements
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = 100.dp.toPx(),
                center = center.copy(x = size.width)
            )
        }
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Summer Surprise", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("50% OFF", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Use code SUMMER50", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
            }
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PremiumColors.Accent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Claim", fontWeight = FontWeight.Bold)
            }
        }
    }
}
