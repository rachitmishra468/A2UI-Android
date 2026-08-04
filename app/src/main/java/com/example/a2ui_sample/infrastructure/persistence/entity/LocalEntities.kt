package com.example.a2ui_sample.infrastructure.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.a2ui_sample.domain.valueobjects.*

@Entity(tableName = "order_history")
data class OrderHistoryEntity(
    @PrimaryKey val orderId: String,
    val userId: String,
    val items: String, // Stored as JSON string
    val totalAmount: Int,
    val paymentMethod: String,
    val orderStatus: String,
    val orderDate: Long,
    val deliveryAddress: String
)

@Entity(tableName = "table_bookings")
data class TableBookingEntity(
    @PrimaryKey val bookingId: String,
    val userId: String,
    val restaurantId: String,
    val restaurantName: String,
    val tableNumber: Int,
    val bookingDate: String,
    val bookingTime: String,
    val guestCount: Int,
    val bookingStatus: String,
    val source: String,
    val createdAt: Long
)

@Entity(tableName = "cart_items")
data class CartEntity(
    @PrimaryKey val cartItemId: Int, // Use itemId as primary key
    val itemName: String,
    val quantity: Int,
    val price: Int,
    val imageUrl: String
)

@Entity(tableName = "customer_feedback")
data class CustomerFeedbackEntity(
    @PrimaryKey val feedbackId: String,
    val orderId: String,
    val rating: Int,
    val comment: String,
    val feedbackDate: Long
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val email: String,
    val phone: String
)

@Entity(tableName = "delivery_tracking")
data class DeliveryTrackingEntity(
    @PrimaryKey val trackingId: String,
    val orderId: String,
    val currentStatus: String,
    val estimatedDeliveryTime: Long,
    val lastUpdated: Long
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val isA2UI: Boolean = false,
    val a2uiPayload: String? = null
)
