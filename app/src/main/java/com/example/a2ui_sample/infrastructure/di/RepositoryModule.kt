package com.example.a2ui_sample.infrastructure.di

import com.example.a2ui_sample.domain.repository.DeliveryRepository
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.example.a2ui_sample.domain.repository.OrderRepository
import com.example.a2ui_sample.domain.repository.ReservationRepository
import com.example.a2ui_sample.infrastructure.repository.DeliveryRepositoryImpl
import com.example.a2ui_sample.infrastructure.repository.MenuRepositoryImpl
import com.example.a2ui_sample.infrastructure.repository.OrderRepositoryImpl
import com.example.a2ui_sample.infrastructure.repository.ReservationRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMenuRepository(
        menuRepositoryImpl: MenuRepositoryImpl
    ): MenuRepository

    @Binds
    @Singleton
    abstract fun bindReservationRepository(
        reservationRepositoryImpl: ReservationRepositoryImpl
    ): ReservationRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(
        orderRepositoryImpl: OrderRepositoryImpl
    ): OrderRepository

    @Binds
    @Singleton
    abstract fun bindDeliveryRepository(
        deliveryRepositoryImpl: DeliveryRepositoryImpl
    ): DeliveryRepository
}
