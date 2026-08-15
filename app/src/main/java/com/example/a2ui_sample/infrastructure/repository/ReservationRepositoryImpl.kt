package com.example.a2ui_sample.infrastructure.repository

import com.example.a2ui_sample.domain.model.Reservation
import com.example.a2ui_sample.domain.repository.ReservationRepository
import com.example.a2ui_sample.domain.valueobjects.*
import com.example.a2ui_sample.infrastructure.persistence.dao.BookingDao
import com.example.a2ui_sample.infrastructure.persistence.entity.TableBookingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ReservationRepositoryImpl
 * Implementation of ReservationRepository using Room.
 */
@Singleton
class ReservationRepositoryImpl @Inject constructor(
    private val bookingDao: BookingDao
) : ReservationRepository {

    override suspend fun createReservation(reservation: Reservation): ReservationId {
        val entity = TableBookingEntity(
            bookingId = reservation.id.value,
            userId = reservation.customerId.value,
            restaurantId = reservation.restaurantId.value,
            restaurantName = reservation.restaurantName,
            tableNumber = reservation.tableId?.value ?: 0,
            bookingDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(reservation.timeSlot.startMillis)),
            bookingTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(reservation.timeSlot.startMillis)),
            guestCount = reservation.partySize,
            bookingStatus = reservation.status.name,
            source = reservation.source.name,
            createdAt = reservation.createdAt
        )
        bookingDao.insertBooking(entity)
        return reservation.id
    }

    override suspend fun getReservationById(id: ReservationId): Reservation? {
        return bookingDao.getBookingById(id.value)?.let { mapToDomain(it) }
    }

    override fun getUpcomingReservations(customerId: CustomerId): Flow<List<Reservation>> {
        return bookingDao.getAllBookings().map { entities ->
            entities.map { mapToDomain(it) }
        }
    }

    override suspend fun cancelReservation(id: ReservationId) {
        val booking = bookingDao.getBookingById(id.value)
        booking?.let {
            bookingDao.insertBooking(it.copy(bookingStatus = ReservationStatus.CANCELLED.name))
        }
    }

    override suspend fun updateReservationStatus(id: ReservationId, status: ReservationStatus) {
        val booking = bookingDao.getBookingById(id.value)
        booking?.let {
            bookingDao.insertBooking(it.copy(bookingStatus = status.name))
        }
    }

    override suspend fun deleteReservation(id: ReservationId) {
        bookingDao.deleteBooking(id.value)
    }

    private fun mapToDomain(entity: TableBookingEntity): Reservation {
        // Mocking timeslot back from date/time strings for simplicity in this demo
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        val date = try { sdf.parse("${entity.bookingDate} ${entity.bookingTime}") } catch(e: Exception) { java.util.Date() }
        val startMillis = date?.time ?: System.currentTimeMillis()
        
        return Reservation(
            id = ReservationId(entity.bookingId),
            customerId = CustomerId(entity.userId),
            restaurantId = RestaurantId(entity.restaurantId),
            restaurantName = entity.restaurantName,
            tableId = TableId(entity.tableNumber),
            partySize = entity.guestCount,
            timeSlot = TimeSlot(startMillis, startMillis + 3600000), // 1 hour duration
            status = ReservationStatus.valueOf(entity.bookingStatus),
            source = BookingSource.valueOf(entity.source),
            createdAt = entity.createdAt
        )
    }
}
