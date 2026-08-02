package com.example.a2ui_sample.domain.service

import com.example.a2ui_sample.domain.model.Table
import com.example.a2ui_sample.domain.valueobjects.TimeSlot
import com.example.a2ui_sample.domain.model.Reservation
import com.example.a2ui_sample.domain.valueobjects.TableStatus

/**
 * AvailabilityValidator Domain Service
 * Encapsulates logic for checking table and restaurant availability.
 */
class AvailabilityValidator {
    fun isTableAvailable(table: Table, timeSlot: TimeSlot, existingReservations: List<Reservation>): Boolean {
        if (table.status == TableStatus.MAINTENANCE) return false
        
        return existingReservations.none { it.tableId == table.id && it.timeSlot.overlaps(timeSlot) }
    }
}
