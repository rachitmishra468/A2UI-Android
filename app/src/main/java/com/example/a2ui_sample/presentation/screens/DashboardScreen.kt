package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a2ui_sample.presentation.components.PremiumCard
import com.example.a2ui_sample.presentation.components.StatCard
import com.example.a2ui_sample.presentation.theme.PremiumColors
import com.example.a2ui_sample.presentation.theme.PremiumSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Summary Stats
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Revenue",
                    value = "₹12.4k",
                    trend = "+8.2%",
                    icon = Icons.Outlined.Payments,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Orders",
                    value = "156",
                    trend = "+14.5%",
                    icon = Icons.Outlined.ShoppingBag,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Performance Chart Card
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Text("Weekly Performance", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(24.dp))
                // Simulated Chart
                Row(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val data = listOf(0.4f, 0.6f, 0.3f, 0.8f, 0.5f, 0.9f, 0.7f)
                    data.forEach { height ->
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .fillMaxHeight(height)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(PremiumColors.Accent)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    days.forEach { day ->
                        Text(day, style = MaterialTheme.typography.labelSmall, color = PremiumColors.Gray500)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recent Activity
            Text(
                "Recent Activity",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            ActivityItem("Order #1363 Confirmed", "2 mins ago", Icons.Default.CheckCircle, PremiumColors.Success)
            ActivityItem("New Booking: Table for 4", "1 hour ago", Icons.Default.CalendarMonth, PremiumColors.Info)
            ActivityItem("Order #1362 Delivered", "3 hours ago", Icons.Default.LocalShipping, PremiumColors.Accent)
        }
    }
}

@Composable
fun ActivityItem(title: String, time: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color) {
    PremiumCard(modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(time, color = PremiumColors.Gray500, fontSize = 12.sp)
            }
        }
    }
}
