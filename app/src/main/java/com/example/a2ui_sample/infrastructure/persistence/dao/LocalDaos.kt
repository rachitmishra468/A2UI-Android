package com.example.a2ui_sample.infrastructure.persistence.dao

import androidx.room.*
import com.example.a2ui_sample.infrastructure.persistence.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderHistoryEntity)

    @Query("SELECT * FROM order_history ORDER BY orderDate DESC")
    fun getAllOrders(): Flow<List<OrderHistoryEntity>>

    @Query("SELECT * FROM order_history WHERE orderId = :orderId")
    suspend fun getOrderById(orderId: String): OrderHistoryEntity?
}

@Dao
interface BookingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: TableBookingEntity)

    @Query("SELECT * FROM table_bookings ORDER BY bookingDate DESC")
    fun getAllBookings(): Flow<List<TableBookingEntity>>

    @Query("SELECT * FROM table_bookings WHERE bookingId = :bookingId")
    suspend fun getBookingById(bookingId: String): TableBookingEntity?
}

@Dao
interface CartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartEntity)

    @Query("SELECT * FROM cart_items")
    fun getCartItems(): Flow<List<CartEntity>>

    @Update
    suspend fun updateCartItem(item: CartEntity)

    @Delete
    suspend fun deleteCartItem(item: CartEntity)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}

@Dao
interface FeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: CustomerFeedbackEntity)

    @Query("SELECT * FROM customer_feedback ORDER BY feedbackDate DESC")
    fun getAllFeedback(): Flow<List<CustomerFeedbackEntity>>
}

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    suspend fun getProfile(userId: String): UserProfileEntity?
}

@Dao
interface DeliveryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracking(tracking: DeliveryTrackingEntity)

    @Query("SELECT * FROM delivery_tracking WHERE orderId = :orderId")
    suspend fun getTrackingByOrderId(orderId: String): DeliveryTrackingEntity?
}

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}
