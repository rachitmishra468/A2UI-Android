package com.example.a2ui_sample.infrastructure.di

import android.content.Context
import androidx.room.Room
import com.example.a2ui_sample.infrastructure.persistence.RestaurantDatabase
import com.example.a2ui_sample.infrastructure.persistence.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RestaurantDatabase {
        return Room.databaseBuilder(
            context,
            RestaurantDatabase::class.java,
            "restaurant_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideMenuDao(database: RestaurantDatabase): MenuDao {
        return database.menuDao()
    }

    @Provides
    fun provideOrderDao(database: RestaurantDatabase): OrderDao = database.orderDao()

    @Provides
    fun provideBookingDao(database: RestaurantDatabase): BookingDao = database.bookingDao()

    @Provides
    fun provideCartDao(database: RestaurantDatabase): CartDao = database.cartDao()

    @Provides
    fun provideFeedbackDao(database: RestaurantDatabase): FeedbackDao = database.feedbackDao()

    @Provides
    fun provideUserProfileDao(database: RestaurantDatabase): UserProfileDao = database.userProfileDao()

    @Provides
    fun provideDeliveryDao(database: RestaurantDatabase): DeliveryDao = database.deliveryDao()

    @Provides
    fun provideChatMessageDao(database: RestaurantDatabase): ChatMessageDao = database.chatMessageDao()

    @Provides
    fun provideConversationMemoryDao(database: RestaurantDatabase): ConversationMemoryDao = database.conversationMemoryDao()
}
