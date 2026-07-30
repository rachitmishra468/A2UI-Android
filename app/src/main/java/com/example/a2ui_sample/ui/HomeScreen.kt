@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.a2ui_sample.presentation.viewmodel.RestaurantViewModel
import com.example.a2ui_sample.presentation.a2ui.MenuA2UISection
import com.example.a2ui_sample.presentation.theme.*

@Composable
fun HomeScreen(navController: NavController, viewModel: RestaurantViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top app bar with toggle and cart
        TopAppBarSection(navController, viewModel)

        // Main content with proper spacing
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Top safe area spacing
            Spacer(modifier = Modifier.height(12.dp))

            // Welcome section
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "🍽️ Our Menu",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    "Explore our delicious collection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Menu Section - Refactored to A2UI
            MenuA2UISection(viewModel)

            // Bottom safe area spacing
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom spacing for navigation bar
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun rememberScrollState(): androidx.compose.foundation.ScrollState {
    return androidx.compose.foundation.rememberScrollState()
}

@Composable
fun TopAppBarSection(navController: NavController, viewModel: RestaurantViewModel) {
    val cartCount = viewModel.getCartItems().size

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RestaurantPrimary,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "🍽️ Restaurant",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                // Cart button with badge
                Box(modifier = Modifier.size(48.dp)) {
                    IconButton(
                        onClick = { navController.navigate("cart") },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Shopping Cart",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (cartCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset((-4).dp, 4.dp)
                                .background(RestaurantSecondary, RoundedCornerShape(50))
                                .size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                cartCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // AI Agent button
                Button(
                    onClick = { navController.navigate("ai") },
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = RestaurantSecondary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(
                        "💬 AI",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
