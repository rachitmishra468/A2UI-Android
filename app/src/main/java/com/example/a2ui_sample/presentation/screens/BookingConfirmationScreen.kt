package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a2ui_sample.presentation.theme.PremiumColors
import com.example.a2ui_sample.presentation.components.PremiumCard
import com.example.a2ui_sample.presentation.components.PremiumButton

@Composable
fun BookingConfirmationScreen(
    onViewHistory: () -> Unit,
    onBackHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(110.dp),
            shape = CircleShape,
            color = PremiumColors.Success.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(70.dp),
                    shape = CircleShape,
                    color = PremiumColors.Success
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
                        tint = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Reservation Confirmed", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "We've reserved a special spot for you.", 
            color = PremiumColors.Gray500, 
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(4.dp)) {
                PremiumInfoRow(Icons.Outlined.CalendarMonth, "Date", "05 Aug 2026")
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = PremiumColors.Gray100)
                PremiumInfoRow(Icons.Outlined.Schedule, "Time", "07:00 PM")
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = PremiumColors.Gray100)
                PremiumInfoRow(Icons.Outlined.Groups, "Party Size", "4 People")
            }
        }
        
        Spacer(modifier = Modifier.height(56.dp))
        
        PremiumButton(
            text = "View All Reservations",
            onClick = onViewHistory,
            modifier = Modifier.fillMaxWidth(),
            containerColor = PremiumColors.Primary
        )
        
        TextButton(onClick = onBackHome, modifier = Modifier.padding(top = 16.dp)) {
            Text("Back to Dashboard", color = PremiumColors.Gray500, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun PremiumInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = PremiumColors.Accent)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, modifier = Modifier.weight(1f), color = PremiumColors.Gray500, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PremiumColors.Gray900)
    }
}
