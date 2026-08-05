package com.example.a2ui_sample.agent

import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.IntentResultWrapper
import com.example.a2ui_sample.domain.model.Reservation
import com.example.a2ui_sample.domain.model.TableBooking
import com.example.a2ui_sample.domain.repository.ReservationRepository
import com.example.a2ui_sample.domain.valueobjects.*
import kotlinx.coroutines.flow.first
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
                        val calendar = java.util.Calendar.getInstance()
                        
                        // Robust parsing for AI extracted dates
                        if (dateStr.contains("tomorrow", ignoreCase = true)) {
                            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                        } else if (!dateStr.contains("today", ignoreCase = true)) {
                            // Try parsing YYYY-MM-DD
                            val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val d = try { dateSdf.parse(dateStr) } catch (e: Exception) { null }
                            if (d != null) {
                                val dCal = java.util.Calendar.getInstance()
                                dCal.time = d
                                calendar.set(java.util.Calendar.YEAR, dCal.get(java.util.Calendar.YEAR))
                                calendar.set(java.util.Calendar.MONTH, dCal.get(java.util.Calendar.MONTH))
                                calendar.set(java.util.Calendar.DAY_OF_MONTH, dCal.get(java.util.Calendar.DAY_OF_MONTH))
                            }
                        }

                        // Try various time formats
                        val timeFormats = listOf("HH:mm", "hh:mm a", "h a")
                        var timeDate: java.util.Date? = null
                        for (fmt in timeFormats) {
                            try {
                                timeDate = SimpleDateFormat(fmt, Locale.US).parse(timeStr)
                                if (timeDate != null) break
                            } catch (e: Exception) { continue }
                        }

                        if (timeDate != null) {
                            val tCal = java.util.Calendar.getInstance()
                            tCal.time = timeDate
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, tCal.get(java.util.Calendar.HOUR_OF_DAY))
                            calendar.set(java.util.Calendar.MINUTE, tCal.get(java.util.Calendar.MINUTE))
                        }

                        val startMillis = calendar.timeInMillis
                        
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
                    AgentResponse.Message("I'd love to help you book a table! 🍽️ For how many people, and at what date and time?")
                }
            }
            com.example.a2ui_sample.domain.model.UserIntent.BOOKING_LIST -> {
                val bookings = repository.getUpcomingReservations(CustomerId("guest")).first()
                if (bookings.isNotEmpty()) {
                    AgentResponse.BookingHistory(bookings, "Here are your upcoming table reservations: 📅")
                } else {
                    AgentResponse.Message("You don't have any upcoming reservations yet. 📅 Would you like to book a table?")
                }
            }
            com.example.a2ui_sample.domain.model.UserIntent.BOOKING_CANCEL -> {
                val bookingId = intent.entities["booking_id"] as? String
                val target = intent.entities["target"] as? String
                val dateStr = intent.entities["date"] as? String

                if (bookingId != null) {
                    repository.cancelReservation(ReservationId(bookingId))
                    AgentResponse.Message("Done! ✅ Your booking has been cancelled. We hope to see you again soon! 😊")
                } else if (target == "all" || dateStr != null) {
                    val allBookings = repository.getUpcomingReservations(CustomerId("guest")).first()
                    val toCancel = if (dateStr != null) {
                        allBookings.filter { b ->
                            val bDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date(b.timeSlot.startMillis))
                            // Simple logic to match "today", "tomorrow" or exact date
                            val targetDate = when {
                                dateStr.contains("tomorrow", true) -> {
                                    val cal = java.util.Calendar.getInstance(); cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                                }
                                dateStr.contains("today", true) -> {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
                                }
                                else -> dateStr
                            }
                            bDate == targetDate
                        }
                    } else allBookings

                    if (toCancel.isNotEmpty()) {
                        toCancel.forEach { repository.cancelReservation(it.id) }
                        AgentResponse.Message("Done! ✅ I've cancelled ${toCancel.size} bookings for you. 😊")
                    } else {
                        AgentResponse.Message("I couldn't find any bookings to cancel for ${dateStr ?: "your request"}. 🤔")
                    }
                } else {
                    AgentResponse.Message("Could you tell me which booking you'd like to cancel? 🤔")
                }
            }
            else -> AgentResponse.Message("I can help you book a table! 🍽️ Just tell me when and for how many people.")
        }
    }
}
