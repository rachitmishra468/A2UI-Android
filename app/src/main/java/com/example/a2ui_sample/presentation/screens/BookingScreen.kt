package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.a2ui_sample.presentation.viewmodel.RestaurantMainViewModel
import org.a2ui.compose.animation.AnimatedButton
import org.a2ui.compose.animation.AnimatedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    onBack: () -> Unit,
    onBookingConfirmed: () -> Unit,
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
                title = { Text("Book a Table") },
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
                .background(Color(0xFFF8F8F8))
                .padding(16.dp)
        ) {
            // 1. Number of Guests
            AnimatedText("Number of Guests", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (guests > 1) guests-- }) { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null) }
                AnimatedText("$guests People", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                IconButton(onClick = { if (guests < 20) guests++ }) { Icon(Icons.Default.AddCircleOutline, contentDescription = null) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Select Date
            AnimatedText("Select Date", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            LazyRow(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dates) { date ->
                    SelectableChip(date, date == selectedDate) { selectedDate = date }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Select Time
            AnimatedText("Select Time", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            LazyRow(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(times) { time ->
                    SelectableChip(time, time == selectedTime) { selectedTime = time }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedButton(
                onClick = {
                    viewModel.bookTable(guests, selectedDate, selectedTime)
                    onBookingConfirmed()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Confirm Booking", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SelectableChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
        contentColor = if (isSelected) Color.White else Color.Black,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), fontWeight = FontWeight.Medium)
    }
}
