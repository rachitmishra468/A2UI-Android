package com.example.a2ui_sample.agent

import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.IntentResultWrapper
import com.example.a2ui_sample.domain.model.Reservation
import com.example.a2ui_sample.domain.model.TableBooking
import com.example.a2ui_sample.domain.repository.ReservationRepository
import com.example.a2ui_sample.domain.valueobjects.*
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * BookingAgent
 * Specialist for table reservations.
 */
class BookingAgent(private val repository: ReservationRepository) {

    suspend fun execute(intent: IntentResultWrapper): AgentResponse {
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
                            restaurantId = RestaurantId("rest_1"),
                            restaurantName = "The Grand Kitchen",
                            tableId = TableId((1..20).random()),
                            timeSlot = TimeSlot(startMillis, startMillis + 3600000),
                            partySize = people,
                            status = ReservationStatus.CONFIRMED,
                            source = BookingSource.CHAT
                        )
                        
                        repository.createReservation(reservation)
                        
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
                        AgentResponse.Error("⚠️ Unable to save booking. Please try again.")
                    }
                } else {
                    AgentResponse.Message("I need a bit more info to book a table. For how many people, and at what date and time?")
                }
            }
            com.example.a2ui_sample.domain.model.UserIntent.BOOKING_LIST -> {
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
