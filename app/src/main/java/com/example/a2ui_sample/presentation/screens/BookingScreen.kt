package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.a2ui_sample.presentation.viewmodel.RestaurantMainViewModel
import com.example.a2ui_sample.presentation.theme.PremiumColors
import com.example.a2ui_sample.presentation.components.PremiumCard
import com.example.a2ui_sample.presentation.components.PremiumButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    onBack: () -> Unit,
    onBookingConfirmed: () -> Unit,
    onViewHistory: () -> Unit,
    viewModel: RestaurantMainViewModel = hiltViewModel()
) {
    var guests by remember { mutableStateOf(2) }
    var selectedDate by remember { mutableStateOf("Today, 05 Aug") }
    var selectedTime by remember { mutableStateOf("07:00 PM") }

    val dates = listOf("Today, 05 Aug", "Tomorrow, 06 Aug", "Fri, 07 Aug", "Sat, 08 Aug")
    val times = listOf("06:00 PM", "06:30 PM", "07:00 PM", "07:30 PM", "08:00 PM", "08:30 PM", "09:00 PM")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reservation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onViewHistory) {
                        Icon(Icons.Outlined.History, contentDescription = "History")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp)
        ) {
            // Guest Selection Card
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Groups, contentDescription = null, tint = PremiumColors.Accent)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Number of Guests", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (guests > 1) guests-- },
                        modifier = Modifier.background(PremiumColors.Gray100, CircleShape)
                    ) { Icon(Icons.Default.Remove, contentDescription = null) }
                    
                    Text("$guests Guests", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    
                    IconButton(
                        onClick = { if (guests < 20) guests++ },
                        modifier = Modifier.background(PremiumColors.Gray100, CircleShape)
                    ) { Icon(Icons.Default.Add, contentDescription = null) }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Date Selection
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = PremiumColors.Accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Date", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            LazyRow(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(dates) { date ->
                    PremiumSelectableChip(date, date == selectedDate) { selectedDate = date }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Time Selection
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = PremiumColors.Accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Time", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            LazyRow(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(times) { time ->
                    PremiumSelectableChip(time, time == selectedTime) { selectedTime = time }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            PremiumButton(
                text = "Confirm Reservation",
                onClick = {
                    viewModel.bookTable(guests, selectedDate, selectedTime) {
                        onBookingConfirmed()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                containerColor = PremiumColors.Primary
            )
        }
    }
}

@Composable
fun PremiumSelectableChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) PremiumColors.Accent else PremiumColors.Gray50,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, PremiumColors.Gray200)
    ) {
        Text(
            label, 
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), 
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else PremiumColors.Gray700,
            fontSize = 14.sp
        )
    }
}
