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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.a2ui_sample.domain.model.Reservation
import com.example.a2ui_sample.presentation.viewmodel.RestaurantMainViewModel
import com.example.a2ui_sample.presentation.theme.PremiumColors
import com.example.a2ui_sample.presentation.components.PremiumCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingHistoryScreen(
    onBack: () -> Unit,
    viewModel: RestaurantMainViewModel = hiltViewModel()
) {
    val bookings by viewModel.allBookings.collectAsState(initial = emptyList())
    val sortedBookings = remember(bookings) {
        bookings.sortedByDescending { it.createdAt }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Reservations", fontWeight = FontWeight.Bold) },
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (sortedBookings.isEmpty()) {
                PremiumEmptyBookingsView()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(sortedBookings, key = { it.id.value }) { booking ->
                        PremiumBookingCard(booking, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumBookingCard(booking: Reservation, viewModel: RestaurantMainViewModel? = null) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val statusColor = getPremiumBookingStatusColor(booking.status.name)
    
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(booking.restaurantName, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = PremiumColors.Gray900)
                Text("REF: ${booking.id.value.take(8).uppercase()}", fontSize = 12.sp, color = PremiumColors.Gray400, fontWeight = FontWeight.Medium)
            }
            
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    booking.status.name,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 1.dp, color = PremiumColors.Gray100)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            PremiumBookingStat(Icons.Outlined.Groups, "${booking.partySize} Guests")
            val sdf = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.US)
            PremiumBookingStat(Icons.Outlined.CalendarMonth, sdf.format(java.util.Date(booking.timeSlot.startMillis)))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Booked via ${booking.source}",
                fontSize = 11.sp,
                color = PremiumColors.Gray400,
                fontWeight = FontWeight.Medium
            )
            
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(36.dp).background(PremiumColors.Error.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = PremiumColors.Error, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Cancel Reservation?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to cancel this booking? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel?.deleteBooking(booking.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumColors.Error)
                ) { Text("Confirm Cancellation", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Keep it", color = PremiumColors.Gray500) }
            }
        )
    }
}

@Composable
fun PremiumBookingStat(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = PremiumColors.Accent)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PremiumColors.Gray700)
    }
}

@Composable
fun PremiumEmptyBookingsView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.EventBusy, contentDescription = null, modifier = Modifier.size(80.dp), tint = PremiumColors.Gray200)
        Spacer(modifier = Modifier.height(24.dp))
        Text("No Reservations Yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Your upcoming table bookings will appear here.", color = PremiumColors.Gray500, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private fun getPremiumBookingStatusColor(status: String): Color {
    return when (status.uppercase()) {
        "CONFIRMED" -> PremiumColors.Success
        "PENDING" -> PremiumColors.Warning
        "CANCELLED" -> PremiumColors.Error
        "COMPLETED" -> PremiumColors.Info
        else -> PremiumColors.Gray400
    }
}
