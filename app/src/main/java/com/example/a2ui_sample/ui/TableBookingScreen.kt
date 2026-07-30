@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.a2ui_sample.domain.model.TableBooking
import com.example.a2ui_sample.presentation.viewmodel.RestaurantViewModel
import com.example.a2ui_sample.presentation.theme.*

/**
 * TableBookingScreen
 * Lets the user book a table MANUALLY (form below) using the SAME BookTableUseCase
 * that the AI agent uses when a prompt like "book table for 5 members at 4pm" is sent.
 * Also lists all bookings made so far (manual or via agent prompt).
 */
private val timeSlots = listOf(
    "12:00 PM", "1:00 PM", "2:00 PM", "4:00 PM",
    "6:00 PM", "7:00 PM", "8:00 PM", "9:00 PM"
)

@Composable
fun TableBookingScreen(navController: NavController? = null, viewModel: RestaurantViewModel = viewModel()) {
    // Recompose when bookings change (agent or manual)
    val updateTrigger = viewModel.cartUpdateTrigger.value

    var peopleCount by remember { mutableStateOf(2) }
    var selectedTime by remember { mutableStateOf(timeSlots.first()) }
    var timeDropdownExpanded by remember { mutableStateOf(false) }
    var confirmationText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = RestaurantPrimary,
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🍽️ Table Booking",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "Book a Table Manually",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                "Tip: you can also just tell the AI Assistant e.g. \"book table for 5 members at 4pm\"",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Number of people stepper
            Text("Number of People", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundGray)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = { if (peopleCount > 1) peopleCount-- },
                    modifier = Modifier.size(40.dp)
                ) { Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold) }

                Text(
                    peopleCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                IconButton(
                    onClick = { if (peopleCount < 20) peopleCount++ },
                    modifier = Modifier.size(40.dp)
                ) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time slot dropdown
            Text("Booking Time", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            ExposedDropdownMenuBox(
                expanded = timeDropdownExpanded,
                onExpandedChange = { timeDropdownExpanded = it },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = selectedTime,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeDropdownExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = timeDropdownExpanded,
                    onDismissRequest = { timeDropdownExpanded = false }
                ) {
                    timeSlots.forEach { slot ->
                        DropdownMenuItem(
                            text = { Text(slot) },
                            onClick = {
                                selectedTime = slot
                                timeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    viewModel.bookTableManually(peopleCount, selectedTime)
                    confirmationText = "✅ Table booked for $peopleCount people at $selectedTime"
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = RestaurantSecondary)
            ) {
                Text("Book Table", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            confirmationText?.let {
                Text(
                    it,
                    color = RestaurantSuccess,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        Divider(color = BorderGray, thickness = 1.dp)

        // Existing bookings list
        val bookings = viewModel.getBookings()
        Text(
            "Your Bookings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(16.dp)
        )

        if (bookings.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📅", fontSize = 40.sp)
                Text(
                    "No table bookings yet",
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bookings.reversed()) { booking ->
                    BookingCard(booking)
                }
            }
        }
    }
}

@Composable
private fun BookingCard(booking: TableBooking) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Booking ID: ${booking.bookingId}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    "${booking.numberOfPeople} people • ${booking.bookingTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(RestaurantSuccess.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Confirmed", color = RestaurantSuccess, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
