package com.example.a2ui_sample.application.usecase

import com.example.a2ui_sample.domain.model.Reservation
import com.example.a2ui_sample.domain.repository.ReservationRepository
import com.example.a2ui_sample.domain.valueobjects.ReservationId
import javax.inject.Inject

/**
 * CreateReservationUseCase
 * Shared business logic for both Manual UI and Agent flows.
 */
class CreateReservationUseCase @Inject constructor(
    private val reservationRepository: ReservationRepository
) {
    suspend operator fun invoke(reservation: Reservation): ReservationId {
        // Here we could add cross-cutting concerns like validation services
        return reservationRepository.createReservation(reservation)
    }
}
