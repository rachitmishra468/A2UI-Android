package com.example.a2ui_sample.domain.model

/**
 * CartItem Entity
 * Represents an item in the shopping cart.
 */
data class CartItem(
    val menuItem: MenuItem,
    var quantity: Int
)
