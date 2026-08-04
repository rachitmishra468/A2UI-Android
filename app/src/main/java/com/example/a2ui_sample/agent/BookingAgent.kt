package com.example.a2ui_sample.agent

import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.IntentResult
import com.example.a2ui_sample.domain.model.Reservation
import com.example.a2ui_sample.domain.model.TableBooking
import com.example.a2ui_sample.domain.repository.ReservationRepository
import com.example.a2ui_sample.domain.valueobjects.*
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * BookingAgent
 * Execution specialist for table reservations.
 */
class BookingAgent(private val repository: ReservationRepository) {

    suspend fun execute(intent: IntentResult): AgentResponse {
        return when (intent.intent) {
            com.example.a2ui_sample.domain.model.UserIntent.BOOKING_CREATE -> {
                val people = (intent.entities["people_count"] as? Number)?.toInt()
                val dateStr = intent.entities["date"] as? String
                val timeStr = intent.entities["time"] as? String
                
                if (people != null && dateStr != null && timeStr != null) {
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                        val date = sdf.parse("$dateStr $timeStr")
                        val startMillis = date?.time ?: System.currentTimeMillis()
                        
                        val reservation = Reservation(
                            id = ReservationId(),
                            customerId = CustomerId("guest"),
                            restaurantId = RestaurantId("rest_1"), // Default for demo
                            restaurantName = "The Grand Kitchen",   // Default for demo
                            tableId = TableId((1..20).random()),
                            timeSlot = TimeSlot(startMillis, startMillis + 3600000),
                            partySize = people,
                            status = ReservationStatus.CONFIRMED,
                            source = BookingSource.CHAT
                        )
                        
                        repository.createReservation(reservation)
                        
                        // Map back to TableBooking for UI compatibility in AgentResponse if needed,
                        // or better, update AgentResponse to handle Reservation.
                        // For now, let's keep TableBooking as a UI model.
                        val tableBooking = TableBooking(
                            id = reservation.id.value,
                            numberOfPeople = reservation.partySize,
                            bookingDate = dateStr,
                            bookingTime = timeStr,
                            tableNumber = reservation.tableId?.value ?: 0,
                            status = reservation.status,
                            createdAt = reservation.createdAt
                        )
                        
                        AgentResponse.BookingConfirmation(tableBooking)
                    } catch (e: Exception) {
                        android.util.Log.e("A2UI_BOOKING", "Failed to save booking", e)
                        AgentResponse.Error("⚠️ Unable to save booking. Please try again.")
                    }
                } else {
                    AgentResponse.Message("I need a bit more info to book a table. For how many people, and at what date and time?")
                }
            }
            com.example.a2ui_sample.domain.model.UserIntent.BOOKING_LIST -> {
                // In a real app, we'd fetch from repository.
                // For this demo, the UI observes getUpcomingReservations(CustomerId("guest"))
                AgentResponse.Message("I've opened your booking history for you.")
            }
            com.example.a2ui_sample.domain.model.UserIntent.BOOKING_CANCEL -> {
                val bookingId = intent.entities["booking_id"] as? String
                if (bookingId != null) {
                    repository.cancelReservation(ReservationId(bookingId))
                    AgentResponse.Message("Your booking $bookingId has been successfully cancelled.")
                } else {
                    AgentResponse.Message("Please provide the Booking ID you wish to cancel.")
                }
            }
            else -> AgentResponse.Message("I can help you with table reservations.")
        }
    }
}
