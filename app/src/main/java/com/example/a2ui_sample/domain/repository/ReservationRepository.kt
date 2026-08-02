package com.example.a2ui_sample.domain.repository

import com.example.a2ui_sample.domain.model.Reservation
import com.example.a2ui_sample.domain.valueobjects.ReservationId
import com.example.a2ui_sample.domain.valueobjects.ReservationStatus
import com.example.a2ui_sample.domain.valueobjects.CustomerId
import kotlinx.coroutines.flow.Flow

/**
 * ReservationRepository Contract
 * Handles table bookings and availability.
 */
interface ReservationRepository {
    suspend fun createReservation(reservation: Reservation): ReservationId
    suspend fun getReservationById(id: ReservationId): Reservation?
    fun getUpcomingReservations(customerId: CustomerId): Flow<List<Reservation>>
    suspend fun cancelReservation(id: ReservationId)
    suspend fun updateReservationStatus(id: ReservationId, status: ReservationStatus)
}
