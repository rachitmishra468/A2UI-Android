package com.example.a2ui_sample.infrastructure.repository

import com.example.a2ui_sample.domain.model.Order
import com.example.a2ui_sample.domain.repository.OrderRepository
import com.example.a2ui_sample.domain.valueobjects.*
import com.example.a2ui_sample.infrastructure.persistence.dao.OrderDao
import com.example.a2ui_sample.infrastructure.persistence.entity.OrderHistoryEntity
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OrderRepositoryImpl
 * Implementation of OrderRepository using Room.
 */
@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val orderDao: OrderDao,
    private val deliveryDao: com.example.a2ui_sample.infrastructure.persistence.dao.DeliveryDao
) : OrderRepository {
    private val gson = Gson()

    override suspend fun placeOrder(order: Order): OrderId {
        val entity = OrderHistoryEntity(
            orderId = order.id.value,
            userId = "guest", // Mocked user ID
            items = gson.toJson(order.items),
            totalAmount = order.totalAmount.amount,
            paymentMethod = "COD",
            orderStatus = order.status.name,
            orderDate = System.currentTimeMillis(),
            deliveryAddress = "Indiranagar, Bangalore"
        )
        orderDao.insertOrder(entity)
        
        // Create initial delivery tracking
        deliveryDao.insertTracking(
            com.example.a2ui_sample.infrastructure.persistence.entity.DeliveryTrackingEntity(
                trackingId = "DEL-${order.id.value}",
                orderId = order.id.value,
                currentStatus = "PENDING",
                estimatedDeliveryTime = System.currentTimeMillis() + 30 * 60 * 1000, // 30 mins later
                lastUpdated = System.currentTimeMillis()
            )
        )

        return order.id
    }

    override suspend fun getOrderById(id: OrderId): Order? {
        return orderDao.getOrderById(id.value)?.let { mapToDomain(it) }
    }

    override fun getActiveOrders(): Flow<List<Order>> {
        return orderDao.getAllOrders().map { entities ->
            entities.filter { it.orderStatus != OrderStatus.COMPLETED.name }
                .map { mapToDomain(it) }
        }
    }

    override fun getAllOrders(): Flow<List<Order>> {
        return orderDao.getAllOrders().map { entities ->
            entities.map { mapToDomain(it) }
        }
    }

    override suspend fun updateOrderStatus(id: OrderId, status: OrderStatus) {
        val order = orderDao.getOrderById(id.value)
        order?.let {
            orderDao.insertOrder(it.copy(orderStatus = status.name))
        }
    }

    override suspend fun getOrderHistory(customerId: CustomerId): List<Order> {
        return orderDao.getAllOrders().first().map { mapToDomain(it) }
    }

    fun getAllOrdersFlow(): Flow<List<Order>> {
        return orderDao.getAllOrders().map { entities ->
            entities.map { mapToDomain(it) }
        }
    }

    private fun mapToDomain(entity: OrderHistoryEntity): Order {
        val itemsType = object : com.google.gson.reflect.TypeToken<List<com.example.a2ui_sample.domain.model.OrderItem>>() {}.type
        val items: List<com.example.a2ui_sample.domain.model.OrderItem> = gson.fromJson(entity.items, itemsType)
        
        return Order(
            id = OrderId(entity.orderId),
            items = items,
            subtotal = Price(entity.totalAmount), // Mocked for now
            tax = Price(0),
            totalAmount = Price(entity.totalAmount),
            status = OrderStatus.valueOf(entity.orderStatus),
            orderTime = entity.orderDate
        )
    }
}
