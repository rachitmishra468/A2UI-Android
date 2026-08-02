package com.example.a2ui_sample.domain.model

import com.example.a2ui_sample.domain.valueobjects.ReservationStatus

/**
 * TableBooking Entity
 * Simple model for in-memory booking storage.
 */
data class TableBooking(
    val id: String = "BK-${System.currentTimeMillis() % 10000}",
    val numberOfPeople: Int,
    val bookingDate: String,
    val bookingTime: String,
    val tableNumber: Int = (1..20).random(),
    val status: ReservationStatus = ReservationStatus.CONFIRMED,
    val createdAt: Long = System.currentTimeMillis()
) {
    val bookingId: String get() = id
}
