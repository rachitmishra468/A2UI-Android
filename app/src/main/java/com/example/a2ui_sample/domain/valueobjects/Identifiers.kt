package com.example.a2ui_sample.domain.valueobjects

import java.util.UUID

/**
 * OrderId - Value Object for Order Identity
 */
data class OrderId(
    val value: String = UUID.randomUUID().toString()
) {
    init {
        require(value.isNotBlank()) { "OrderId cannot be blank" }
    }

    override fun toString(): String = "ORD-${value.take(8).uppercase()}"
}

/**
 * ReservationId - Value Object for Reservation Identity
 */
data class ReservationId(
    val value: String = UUID.randomUUID().toString()
) {
    init {
        require(value.isNotBlank()) { "ReservationId cannot be blank" }
    }

    override fun toString(): String = "RES-${value.take(8).uppercase()}"
}

/**
 * DeliveryId - Value Object for Delivery Identity
 */
data class DeliveryId(
    val value: String = UUID.randomUUID().toString()
) {
    init {
        require(value.isNotBlank()) { "DeliveryId cannot be blank" }
    }

    override fun toString(): String = "DEL-${value.take(8).uppercase()}"
}

/**
 * FeedbackId - Value Object for Feedback Identity
 */
data class FeedbackId(
    val value: String = UUID.randomUUID().toString()
) {
    init {
        require(value.isNotBlank()) { "FeedbackId cannot be blank" }
    }

    override fun toString(): String = "FB-${value.take(8).uppercase()}"
}

/**
 * TableId - Value Object for Table Identity
 */
data class TableId(
    val value: Int
) {
    init {
        require(value > 0) { "TableId must be positive" }
    }

    override fun toString(): String = "TBL-$value"
}

/**
 * RestaurantId - Value Object for Restaurant Identity
 */
data class RestaurantId(
    val value: String = UUID.randomUUID().toString()
) {
    init {
        require(value.isNotBlank()) { "RestaurantId cannot be blank" }
    }

    override fun toString(): String = "REST-${value.take(6).uppercase()}"
}

/**
 * CustomerId - Value Object for Customer Identity
 */
data class CustomerId(
    val value: String = UUID.randomUUID().toString()
) {
    init {
        require(value.isNotBlank()) { "CustomerId cannot be blank" }
    }

    override fun toString(): String = "CUST-${value.take(6).uppercase()}"
}

