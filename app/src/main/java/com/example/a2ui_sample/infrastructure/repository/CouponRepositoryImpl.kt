package com.example.a2ui_sample.infrastructure.repository

import com.example.a2ui_sample.domain.model.Coupon
import com.example.a2ui_sample.domain.repository.CouponRepository
import com.example.a2ui_sample.domain.valueobjects.CouponId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CouponRepositoryImpl @Inject constructor() : CouponRepository {

    private val coupons = mutableListOf(
        Coupon(
            id = CouponId(),
            code = "WELCOME30",
            description = "Welcome offer - 30% off (max ₹500) on orders above ₹200",
            discountPercentage = 30,
            minOrderAmount = 200f,
            maxDiscount = 500f,
            expiryDate = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000)
        ),
        Coupon(
            id = CouponId(),
            code = "SAVE20",
            description = "Save 20% (max ₹400) on orders above ₹300",
            discountPercentage = 20,
            minOrderAmount = 300f,
            maxDiscount = 400f,
            expiryDate = System.currentTimeMillis() + (60L * 24 * 60 * 60 * 1000)
        ),
        Coupon(
            id = CouponId(),
            code = "FLAT50",
            description = "Flat 15% off (max ₹50) on orders above ₹400",
            discountPercentage = 15,
            minOrderAmount = 400f,
            maxDiscount = 50f,
            expiryDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
        ),
        Coupon(
            id = CouponId(),
            code = "FRIDAY25",
            description = "Friday Special - 25% off (max ₹600) on orders above ₹250",
            discountPercentage = 25,
            minOrderAmount = 250f,
            maxDiscount = 600f,
            expiryDate = System.currentTimeMillis() + (14L * 24 * 60 * 60 * 1000)
        )
    )

    override suspend fun getAvailableCoupons(): List<Coupon> {
        return coupons.filter { it.isValid() }
    }

    override suspend fun getCouponByCode(code: String): Coupon? {
        return coupons.find { it.code.equals(code, ignoreCase = true) }
    }

    override suspend fun incrementUsage(code: String): Boolean {
        val index = coupons.indexOfFirst { it.code.equals(code, ignoreCase = true) }
        if (index != -1) {
            val coupon = coupons[index]
            coupons[index] = coupon.copy(currentUsage = coupon.currentUsage + 1)
            return true
        }
        return false
    }
}
