package com.example.a2ui_sample.infrastructure.repository

import com.example.a2ui_sample.domain.model.Reservation
import com.example.a2ui_sample.domain.repository.ReservationRepository
import com.example.a2ui_sample.domain.valueobjects.CustomerId
import com.example.a2ui_sample.domain.valueobjects.ReservationId
import com.example.a2ui_sample.domain.valueobjects.ReservationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ReservationRepositoryImpl
 * Implementation of ReservationRepository.
 */
@Singleton
class ReservationRepositoryImpl @Inject constructor() : ReservationRepository {

    override suspend fun createReservation(reservation: Reservation): ReservationId {
        return reservation.id
    }

    override suspend fun getReservationById(id: ReservationId): Reservation? {
        return null
    }

    override fun getUpcomingReservations(customerId: CustomerId): Flow<List<Reservation>> {
        return flowOf(emptyList())
    }

    override suspend fun cancelReservation(id: ReservationId) {
        // Implementation
    }

    override suspend fun updateReservationStatus(id: ReservationId, status: ReservationStatus) {
        // Implementation
    }
}
