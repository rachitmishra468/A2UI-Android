package com.example.a2ui_sample.ai_assistant.tools

import android.util.Log
import com.example.a2ui_sample.domain.repository.CouponRepository
import com.google.adk.kt.annotations.Tool
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CouponTools @Inject constructor(
    private val couponRepository: CouponRepository
) {

    @Tool(
        name = "get_available_coupons",
        description = "Show all active discount coupons available for the user."
    )
    suspend fun getAvailableCoupons(): Map<String, Any?> {
        Log.d("AssistantFlow", "TOOL: getAvailableCoupons() called")
        val coupons = couponRepository.getAvailableCoupons()
        return mapOf(
            "result" to mapOf(
                "coupons" to coupons.map { 
                    mapOf(
                        "code" to it.code,
                        "description" to it.description,
                        "discount_percentage" to it.discountPercentage,
                        "min_order_amount" to it.minOrderAmount,
                        "max_discount" to it.maxDiscount
                    )
                },
                "message" to if (coupons.isEmpty()) "No coupons available right now." else "Here are the available coupons."
            )
        )
    }

    @Tool(
        name = "validate_coupon",
        description = "Check if a coupon code is valid and usable for an order."
    )
    suspend fun validateCoupon(couponCode: String): Map<String, Any?> {
        Log.d("AssistantFlow", "TOOL: validate_coupon(code=$couponCode) called")
        val coupon = couponRepository.getCouponByCode(couponCode)
        
        return if (coupon != null && coupon.isValid()) {
            mapOf(
                "valid" to true,
                "code" to coupon.code,
                "description" to coupon.description,
                "percentage" to coupon.discountPercentage,
                "minOrder" to coupon.minOrderAmount,
                "message" to "Coupon '$couponCode' is valid!"
            )
        } else {
            mapOf(
                "valid" to false,
                "message" to "Coupon code '$couponCode' is invalid, expired, or limit reached."
            )
        }
    }

    @Tool(
        name = "apply_coupon",
        description = "Calculate the discount and final amount when applying a coupon to an order amount."
    )
    suspend fun applyCoupon(couponCode: String, orderAmount: Double): Map<String, Any?> {
        Log.d("AssistantFlow", "TOOL: apply_coupon(code=$couponCode, amount=$orderAmount) called")
        val coupon = couponRepository.getCouponByCode(couponCode)
        
        if (coupon == null) {
            return mapOf("success" to false, "message" to "Coupon not found.")
        }

        if (!coupon.isValid(orderAmount.toFloat())) {
            return if (orderAmount < coupon.minOrderAmount) {
                mapOf(
                    "success" to false, 
                    "message" to "Minimum order of ₹${coupon.minOrderAmount} required for this coupon. Your total is ₹$orderAmount."
                )
            } else {
                mapOf("success" to false, "message" to "Coupon is no longer valid.")
            }
        }

        val discount = coupon.calculateDiscount(orderAmount.toFloat())
        val finalAmount = orderAmount - discount

        return mapOf(
            "success" to true,
            "couponCode" to couponCode,
            "discountAmount" to discount,
            "finalAmount" to finalAmount,
            "message" to "Coupon applied! You save ₹$discount. Final amount: ₹$finalAmount."
        )
    }
}
