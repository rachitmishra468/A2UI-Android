package com.example.a2ui_sample.domain.model

import com.example.a2ui_sample.domain.valueobjects.CouponId

data class Coupon(
    val id: CouponId = CouponId(),
    val code: String,                    // "WELCOME30"
    val description: String,              // "Welcome offer - 30% off"
    val discountPercentage: Int,          // 30
    val minOrderAmount: Float = 0f,       // ₹200
    val maxDiscount: Float = Float.MAX_VALUE,  // ₹500
    val expiryDate: Long,                 // Timestamp
    val usageLimit: Int = -1,             // -1 = unlimited
    val currentUsage: Int = 0,            // Usage counter
    val isActive: Boolean = true
) {
    fun isValid(orderAmount: Float? = null): Boolean {
        val now = System.currentTimeMillis()
        val basicValid = isActive && 
               expiryDate > now && 
               (usageLimit == -1 || currentUsage < usageLimit)
        
        return if (orderAmount != null) {
            basicValid && orderAmount >= minOrderAmount
        } else {
            basicValid
        }
    }

    fun calculateDiscount(orderAmount: Float): Float {
        if (!isValid(orderAmount)) return 0f
        val discount = (orderAmount * discountPercentage) / 100f
        return minOf(discount, maxDiscount)
    }
}
