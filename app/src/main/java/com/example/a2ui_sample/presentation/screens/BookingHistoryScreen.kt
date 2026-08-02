package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.a2ui_sample.domain.model.TableBooking
import com.example.a2ui_sample.domain.valueobjects.ReservationStatus
import com.example.a2ui_sample.presentation.viewmodel.RestaurantMainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingHistoryScreen(
    onBack: () -> Unit,
    viewModel: RestaurantMainViewModel = hiltViewModel()
) {
    val bookings = viewModel.getBookings()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (bookings.isEmpty()) {
            EmptyState("No bookings found", Icons.Default.CalendarToday, padding)
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFFF8F8F8)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(bookings) { booking ->
                    BookingCard(booking)
                }
            }
        }
    }
}

@Composable
fun BookingCard(booking: TableBooking) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(booking.bookingDate, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                StatusBadge(booking.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Text(booking.bookingTime, modifier = Modifier.padding(start = 8.dp), color = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Text("${booking.numberOfPeople} People", modifier = Modifier.padding(start = 8.dp), color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Table #${booking.tableNumber}", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
                Text("ID: ${booking.id}", fontSize = 12.sp, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun StatusBadge(status: ReservationStatus) {
    val color = when(status) {
        ReservationStatus.CONFIRMED -> Color(0xFF4CAF50)
        ReservationStatus.PENDING -> Color(0xFFFF9800)
        ReservationStatus.CANCELLED -> Color(0xFFF44336)
        ReservationStatus.COMPLETED -> Color(0xFF2196F3)
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            status.name,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector, padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(100.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, fontSize = 20.sp, color = Color.Gray)
    }
}
