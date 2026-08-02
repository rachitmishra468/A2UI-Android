package com.example.a2ui_sample.infrastructure.repository

import com.example.a2ui_sample.domain.model.Delivery
import com.example.a2ui_sample.domain.repository.DeliveryRepository
import com.example.a2ui_sample.domain.valueobjects.DeliveryId
import com.example.a2ui_sample.domain.valueobjects.DeliveryStatus
import com.example.a2ui_sample.domain.valueobjects.OrderId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeliveryRepositoryImpl
 * Implementation of DeliveryRepository.
 */
@Singleton
class DeliveryRepositoryImpl @Inject constructor() : DeliveryRepository {

    override suspend fun getDeliveryByOrderId(orderId: OrderId): Delivery? {
        return null
    }

    override fun trackDelivery(id: DeliveryId): Flow<Delivery> {
        return emptyFlow()
    }

    override suspend fun updateDeliveryStatus(id: DeliveryId, status: DeliveryStatus) {
        // Implementation
    }
}
