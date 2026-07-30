package com.example.a2ui_sample.domain.valueobjects

import kotlin.math.absoluteValue

/**
 * Price Value Object
 * Ensures price is always valid (non-negative)
 * Handles currency conversions and calculations
 */
data class Price(
    val amount: Int
) {
    init {
        require(amount >= 0) { "Price amount must be non-negative, got: $amount" }
    }

    fun add(other: Price): Price = Price(amount + other.amount)

    fun multiply(quantity: Int): Price {
        require(quantity >= 0) { "Quantity must be non-negative" }
        return Price(amount * quantity)
    }

    fun applyDiscount(discountPercent: Int): Price {
        require(discountPercent in 0..100) { "Discount must be between 0 and 100" }
        val discountAmount = (amount * discountPercent) / 100
        return Price(amount - discountAmount)
    }

    fun applyTax(taxPercent: Int): Price {
        require(taxPercent >= 0) { "Tax must be non-negative" }
        val taxAmount = (amount * taxPercent) / 100
        return Price(amount + taxAmount)
    }

    fun isValidPrice(): Boolean = amount >= 0

    override fun toString(): String = "₹$amount"
}

