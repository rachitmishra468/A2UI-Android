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
class DeliveryRepositoryImpl @Inject constructor(
    private val deliveryDao: com.example.a2ui_sample.infrastructure.persistence.dao.DeliveryDao
) : DeliveryRepository {

    override suspend fun getDeliveryByOrderId(orderId: OrderId): Delivery? {
        val entity = deliveryDao.getTrackingByOrderId(orderId.value)
        return entity?.let {
            val status = try {
                DeliveryStatus.valueOf(it.currentStatus)
            } catch (e: Exception) {
                DeliveryStatus.PENDING
            }
            Delivery(
                id = DeliveryId(it.trackingId),
                orderId = OrderId(it.orderId),
                status = status,
                estimatedArrivalAt = it.estimatedDeliveryTime,
                deliveryAddress = "Indiranagar, Bangalore" // Mocked as it's not in the tracking entity
            )
        }
    }

    override fun trackDelivery(id: DeliveryId): Flow<Delivery> {
        return emptyFlow()
    }

    override suspend fun updateDeliveryStatus(id: DeliveryId, status: DeliveryStatus) {
        // Implementation
    }
}
