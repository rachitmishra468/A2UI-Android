package com.example.a2ui_sample.domain.model

import com.example.a2ui_sample.domain.valueobjects.ReservationId
import com.example.a2ui_sample.domain.valueobjects.ReservationStatus
import com.example.a2ui_sample.domain.valueobjects.CustomerId
import com.example.a2ui_sample.domain.valueobjects.TableId
import com.example.a2ui_sample.domain.valueobjects.TimeSlot

/**
 * Reservation Entity
 * Represents a table booking.
 */
data class Reservation(
    val id: ReservationId,
    val customerId: CustomerId,
    val tableId: TableId?,
    val timeSlot: TimeSlot,
    val partySize: Int,
    val status: ReservationStatus = ReservationStatus.PENDING,
    val specialRequests: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
