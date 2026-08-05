package com.example.a2ui_sample.infrastructure.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.a2ui_sample.infrastructure.persistence.dao.*
import com.example.a2ui_sample.infrastructure.persistence.entity.*

@Database(
    entities = [
        OrderHistoryEntity::class,
        TableBookingEntity::class,
        CartEntity::class,
        CustomerFeedbackEntity::class,
        UserProfileEntity::class,
        DeliveryTrackingEntity::class,
        MenuItemEntity::class,
        ChatMessageEntity::class,
    ],
    version = 5, // Incremented for conversationId field
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RestaurantDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun bookingDao(): BookingDao
    abstract fun cartDao(): CartDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun menuDao(): MenuDao
    abstract fun chatMessageDao(): ChatMessageDao
}
