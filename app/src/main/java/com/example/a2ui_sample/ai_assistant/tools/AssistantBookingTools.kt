package com.example.a2ui_sample.ai_assistant.tools

import com.example.a2ui_sample.domain.model.Reservation
import com.example.a2ui_sample.domain.repository.ReservationRepository
import com.example.a2ui_sample.domain.valueobjects.*
import com.google.adk.kt.annotations.Tool
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantBookingTools @Inject constructor(
    private val reservationRepository: ReservationRepository
) {
    @Tool(
        name = "create_booking",
        description = "Book a table at the restaurant."
    )
    suspend fun createBooking(date: String, time: String, peopleCount: Int): String {
        return try {
            val reservation = Reservation(
                id = ReservationId(),
                customerId = CustomerId("guest"),
                restaurantId = RestaurantId("rest_1"),
                restaurantName = "Luxe Dining",
                tableId = TableId((1..20).random()),
                timeSlot = TimeSlot(System.currentTimeMillis(), System.currentTimeMillis() + 3600000), // Placeholder
                partySize = peopleCount,
                status = ReservationStatus.CONFIRMED,
                source = BookingSource.CHAT
            )
            reservationRepository.createReservation(reservation)
            "✅ Table booked successfully for $peopleCount people on $date at $time."
        } catch (e: Exception) {
            "❌ Failed to create booking: ${e.message}"
        }
    }

    @Tool(
        name = "list_bookings",
        description = "View your current table reservations."
    )
    suspend fun listBookings(): String {
        val bookings = reservationRepository.getUpcomingReservations(CustomerId("guest")).first()
        if (bookings.isEmpty()) return "You have no active bookings."
        return "Your Bookings:\n" + bookings.joinToString("\n") { 
            "- Table for ${it.partySize} (Status: ${it.status})"
        }
    }
}
