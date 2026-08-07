package com.example.a2ui_sample.infrastructure.di

import com.example.a2ui_sample.agent.ADKRestaurantMasterAgent
import com.example.a2ui_sample.agent.ConversationMemoryManager
import com.example.a2ui_sample.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AgentModule {

    @Provides
    @Singleton
    fun provideADKRestaurantMasterAgent(
        menuRepository: MenuRepository,
        feedbackRepository: FeedbackRepository,
        reservationRepository: ReservationRepository,
        orderRepository: OrderRepository,
        deliveryRepository: DeliveryRepository,
        memoryManager: ConversationMemoryManager
    ): ADKRestaurantMasterAgent {
        return ADKRestaurantMasterAgent(
            menuRepository,
            feedbackRepository,
            reservationRepository,
            orderRepository,
            deliveryRepository,
            memoryManager
        )
    }
}
