package com.example.a2ui_sample.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a2ui_sample.application.usecase.CreateReservationUseCase
import com.example.a2ui_sample.domain.model.Reservation
import com.example.a2ui_sample.domain.valueobjects.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ReservationViewModel
 * Handles the manual reservation flow.
 * Shares CreateReservationUseCase with the ReservationAgent.
 */
@HiltViewModel
class ReservationViewModel @Inject constructor(
    private val createReservationUseCase: CreateReservationUseCase
) : ViewModel() {

    private val _reservationState = MutableStateFlow<ReservationStatus?>(null)
    val reservationState: StateFlow<ReservationStatus?> = _reservationState.asStateFlow()

    fun makeReservation(partySize: Int, startTime: Long) {
        viewModelScope.launch {
            val reservation = Reservation(
                id = ReservationId(),
                customerId = CustomerId("MANUAL_USER"),
                restaurantId = RestaurantId("rest_1"),
                restaurantName = "The Grand Kitchen",
                tableId = null, // To be assigned by domain service
                timeSlot = TimeSlot(startTime, startTime + 3600000),
                partySize = partySize,
                status = ReservationStatus.CONFIRMED,
                source = BookingSource.APP
            )
            createReservationUseCase(reservation)
            _reservationState.value = ReservationStatus.CONFIRMED
        }
    }
}
