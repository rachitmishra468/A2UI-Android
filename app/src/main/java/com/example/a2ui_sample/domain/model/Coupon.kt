package com.example.a2ui_sample.domain.model

import com.example.a2ui_sample.domain.valueobjects.CouponId
import java.util.Date

/**
 * Coupon Entity
 * Represents a discount coupon for the restaurant.
 */
data class Coupon(
    val id: CouponId,
    val code: String,                    // e.g., "WELCOME30"
    val description: String,              // e.g., "Get 30% off on your first order"
    val discountPercentage: Int,          // e.g., 30 for 30%
    val minOrderAmount: Float = 0f,       // Minimum order amount to apply
    val maxDiscount: Float = Float.MAX_VALUE,  // Maximum discount amount
    val expiryDate: Long,                 // Timestamp when coupon expires
    val usageLimit: Int = -1,             // -1 means unlimited
    val currentUsage: Int = 0,            // How many times used
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isValid(): Boolean {
        return isActive &&
               expiryDate > System.currentTimeMillis() &&
               (usageLimit == -1 || currentUsage < usageLimit)
    }

    fun calculateDiscount(orderAmount: Float): Float {
        if (!isValid() || orderAmount < minOrderAmount) return 0f
        val discount = (orderAmount * discountPercentage) / 100f
        return minOf(discount, maxDiscount)
    }
}

