package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BookingConfirmationScreen(
    onViewHistory: () -> Unit,
    onBackHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = Color(0xFF4CAF50)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Table Reserved!", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("We've sent a confirmation to your email.", color = Color.Gray, fontWeight = FontWeight.Medium)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(Icons.Default.CalendarToday, "Date", "05 Aug 2026")
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow(Icons.Default.Check, "Time", "07:00 PM")
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow(Icons.Default.Check, "Guests", "4 People")
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow(Icons.Default.Check, "Table", "Table #12")
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onViewHistory,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("View Booking History", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        TextButton(onClick = onBackHome, modifier = Modifier.padding(top = 16.dp)) {
            Text("Back to Home", fontSize = 16.sp)
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), color = Color.Gray)
        Text(value, fontWeight = FontWeight.Bold)
    }
}
