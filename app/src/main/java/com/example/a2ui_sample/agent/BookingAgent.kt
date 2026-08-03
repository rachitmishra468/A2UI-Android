package com.example.a2ui_sample.agent

import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.IntentResult
import com.example.a2ui_sample.domain.model.TableBooking
import com.example.a2ui_sample.domain.repository.MenuRepository

/**
 * BookingAgent
 * Execution specialist for table reservations.
 */
class BookingAgent(private val repository: MenuRepository) {

    fun execute(intent: IntentResult): AgentResponse {
        return when (intent.intent) {
            com.example.a2ui_sample.domain.model.UserIntent.BOOKING_CREATE -> {
                val people = (intent.entities["people_count"] as? Number)?.toInt()
                val date = intent.entities["date"] as? String
                val time = intent.entities["time"] as? String
                
                if (people != null && date != null && time != null) {
                    val booking = TableBooking(
                        numberOfPeople = people,
                        bookingDate = date,
                        bookingTime = time
                    )
                    repository.addBooking(booking)
                    AgentResponse.BookingConfirmation(booking)
                } else {
                    AgentResponse.Message("I need a bit more info to book a table. For how many people, and at what date and time?")
                }
            }
            com.example.a2ui_sample.domain.model.UserIntent.BOOKING_LIST -> {
                val bookings = repository.getBookings()
                if (bookings.isEmpty()) {
                    AgentResponse.Message("You don't have any active bookings.")
                } else {
                    AgentResponse.Message("You have ${bookings.size} active bookings.")
                }
            }
            com.example.a2ui_sample.domain.model.UserIntent.BOOKING_CANCEL -> {
                val bookingId = intent.entities["booking_id"] as? String
                if (bookingId != null) {
                    val success = repository.cancelBooking(bookingId)
                    if (success) {
                        AgentResponse.Message("Your booking $bookingId has been successfully cancelled.")
                    } else {
                        AgentResponse.Message("I couldn't find a booking with ID $bookingId to cancel.")
                    }
                } else {
                    val bookings = repository.getBookings()
                    if (bookings.isEmpty()) {
                        AgentResponse.Message("You don't have any active bookings to cancel.")
                    } else {
                        val latestId = bookings.last().id
                        repository.cancelBooking(latestId)
                        AgentResponse.Message("I've cancelled your most recent booking ($latestId).")
                    }
                }
            }
            else -> AgentResponse.Message("I can help you with table reservations.")
        }
    }
}
