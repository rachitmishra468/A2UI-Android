package com.example.a2ui_sample.domain.valueobjects

/**
 * Status - Value Object representing entity statuses
 */
enum class ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY,
    PICKED_UP,
    DELIVERED,
    CANCELLED,
    COMPLETED
}

enum class DeliveryStatus {
    PENDING,
    ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED
}

enum class TableStatus {
    AVAILABLE,
    OCCUPIED,
    RESERVED,
    MAINTENANCE
}

enum class BookingSource {
    CHAT,
    APP
}

enum class MenuItemType {
    VEG,
    NONVEG,
    VEGAN,
    BEVERAGE,
    DESSERT
}

/**
 * Rating - Value Object for feedback ratings
 */
data class Rating(
    val value: Int
) {
    init {
        require(value in 1..5) { "Rating must be between 1 and 5, got: $value" }
    }

    fun isPositive(): Boolean = value >= 4
    fun isNeutral(): Boolean = value == 3
    fun isNegative(): Boolean = value <= 2

    override fun toString(): String = "⭐ $value/5"
}

/**
 * TimeSlot - Value Object for time periods
 */
data class TimeSlot(
    val startMillis: Long,
    val endMillis: Long
) {
    init {
        require(startMillis > 0) { "Start time must be positive" }
        require(endMillis > startMillis) { "End time must be after start time" }
    }

    fun durationMinutes(): Long = (endMillis - startMillis) / (1000 * 60)

    fun overlaps(other: TimeSlot): Boolean {
        return startMillis < other.endMillis && endMillis > other.startMillis
    }

    override fun toString(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
        return "${sdf.format(startMillis)} - ${sdf.format(endMillis)}"
    }
}

/**
 * Capacity - Value Object for party/table capacity
 */
data class Capacity(
    val value: Int
) {
    init {
        require(value > 0) { "Capacity must be positive" }
    }

    fun canAccommodate(partySize: Int): Boolean = partySize <= value

    override fun toString(): String = "$value people"
}

/**
 * Quantity - Value Object for item quantities
 */
data class Quantity(
    val value: Int
) {
    init {
        require(value > 0) { "Quantity must be positive" }
    }

    fun add(other: Quantity): Quantity = Quantity(value + other.value)

    override fun toString(): String = "×$value"
}

