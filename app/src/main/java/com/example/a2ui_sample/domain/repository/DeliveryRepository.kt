package com.example.a2ui_sample.domain.repository

import com.example.a2ui_sample.domain.model.Delivery
import com.example.a2ui_sample.domain.valueobjects.DeliveryId
import com.example.a2ui_sample.domain.valueobjects.OrderId
import com.example.a2ui_sample.domain.valueobjects.DeliveryStatus
import kotlinx.coroutines.flow.Flow

/**
 * DeliveryRepository Contract
 * Handles tracking and delivery logistics.
 */
interface DeliveryRepository {
    suspend fun getDeliveryByOrderId(orderId: OrderId): Delivery?
    fun trackDelivery(id: DeliveryId): Flow<Delivery>
    suspend fun updateDeliveryStatus(id: DeliveryId, status: DeliveryStatus)
}
