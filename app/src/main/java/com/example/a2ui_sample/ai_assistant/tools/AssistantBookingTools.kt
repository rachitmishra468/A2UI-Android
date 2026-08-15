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
    suspend fun createBooking(date: String, time: String, peopleCount: Int): Map<String, Any?> {
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
            mapOf(
                "message" to "Table booked successfully for $peopleCount people on $date at $time.",
                "date" to date,
                "time" to time,
                "guests" to peopleCount,
                "success" to true
            )
        } catch (e: Exception) {
            mapOf(
                "message" to "Failed to create booking: ${e.message}",
                "success" to false
            )
        }
    }

    @Tool(
        name = "list_bookings",
        description = "View your current table reservations with their IDs and times."
    )
    suspend fun listBookings(): Map<String, Any?> {
        val bookings = reservationRepository.getUpcomingReservations(CustomerId("guest")).first()
        return if (bookings.isEmpty()) {
            mapOf("message" to "You have no active bookings.", "bookings" to emptyList<Reservation>())
        } else {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
            val bookingList = bookings.map { 
                mapOf(
                    "id" to it.id.value,
                    "time" to sdf.format(java.util.Date(it.timeSlot.startMillis)),
                    "guests" to it.partySize,
                    "status" to it.status.name
                )
            }
            mapOf(
                "message" to "Found ${bookings.size} bookings.",
                "bookings" to bookingList
            )
        }
    }

    @Tool(
        name = "cancel_booking",
        description = "Cancel a specific table reservation by its booking ID."
    )
    suspend fun cancelBooking(bookingId: String): Map<String, Any?> {
        return try {
            reservationRepository.cancelReservation(ReservationId(bookingId))
            mapOf(
                "message" to "Successfully cancelled reservation $bookingId.",
                "success" to true,
                "bookingId" to bookingId
            )
        } catch (e: Exception) {
            mapOf(
                "message" to "Failed to cancel reservation: ${e.message}",
                "success" to false
            )
        }
    }
}
