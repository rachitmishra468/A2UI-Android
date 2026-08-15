package com.example.a2ui_sample.domain.repository

import com.example.a2ui_sample.domain.model.Coupon

interface CouponRepository {
    suspend fun getAvailableCoupons(): List<Coupon>
    suspend fun getCouponByCode(code: String): Coupon?
    suspend fun incrementUsage(code: String): Boolean
}
