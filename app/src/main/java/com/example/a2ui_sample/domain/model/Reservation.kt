package com.example.a2ui_sample.domain.model

import com.example.a2ui_sample.domain.valueobjects.*

/**
 * Reservation Entity
 * Represents a table booking.
 */
data class Reservation(
    val id: ReservationId,
    val customerId: CustomerId,
    val restaurantId: RestaurantId,
    val restaurantName: String,
    val tableId: TableId?,
    val timeSlot: TimeSlot,
    val partySize: Int,
    val status: ReservationStatus = ReservationStatus.PENDING,
    val source: BookingSource = BookingSource.APP,
    val specialRequests: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
