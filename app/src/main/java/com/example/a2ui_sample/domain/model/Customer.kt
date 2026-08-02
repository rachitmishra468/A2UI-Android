package com.example.a2ui_sample.domain.model

import com.example.a2ui_sample.domain.valueobjects.CustomerId

/**
 * Customer Entity
 * Represents a user of the system.
 */
data class Customer(
    val id: CustomerId,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val loyaltyPoints: Int = 0,
    val preferences: List<String> = emptyList()
)
