package com.example.a2ui_sample.domain.service

import com.example.a2ui_sample.domain.valueobjects.Price
import com.example.a2ui_sample.domain.model.Order

/**
 * PriceCalculator Domain Service
 * Handles complex pricing calculations including taxes and fees.
 */
class PriceCalculator {
    fun calculateTotal(order: Order, taxRatePercent: Int = 5, serviceFee: Price = Price(50)): Price {
        val subtotal = order.totalAmount
        val tax = subtotal.applyTax(taxRatePercent).amount - subtotal.amount
        return subtotal.add(Price(tax)).add(serviceFee)
    }
}
