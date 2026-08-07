package com.example.a2ui_sample.domain.usecases

import com.example.a2ui_sample.domain.model.*
import com.example.a2ui_sample.domain.repository.*
import com.example.a2ui_sample.domain.valueobjects.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * GetFeaturedItemsUseCase
 * Fetches the best-selling menu items.
 */
class GetFeaturedItemsUseCase @Inject constructor(
    private val repository: MenuRepository
) {
    suspend operator fun invoke(): List<MenuItem> {
        return repository.getMenuItems().filter { it.isBestSeller }
    }
}

/**
 * AddToCartUseCase (Action)
 * Adds a specific menu item to the cart.
 */
class AddToCartSimpleUseCase @Inject constructor(
    private val repository: MenuRepository
) {
    suspend operator fun invoke(menuItemId: Int) {
        repository.addToCart(menuItemId)
    }
}

/**
 * UpdateCartQuantityUseCase
 */
class UpdateCartQuantityUseCase @Inject constructor(
    private val repository: MenuRepository
) {
    suspend operator fun invoke(menuItemId: Int, quantity: Int) {
        repository.updateCartQuantity(menuItemId, quantity)
    }
}

/**
 * RemoveFromCartUseCase
 */
class RemoveFromCartUseCase @Inject constructor(
    private val repository: MenuRepository
) {
    suspend operator fun invoke(menuItemId: Int) {
        repository.removeFromCart(menuItemId)
    }
}

/**
 * BookTableUseCase (Domain Action)
 */
class BookTableActionUseCase @Inject constructor(
    private val reservationRepository: ReservationRepository
) {
    suspend operator fun invoke(reservation: Reservation) {
        reservationRepository.createReservation(reservation)
    }
}

/**
 * CheckoutUseCase (Core Logic)
 * Transforms cart into an order and persists it.
 */
class CheckoutOrderUseCase @Inject constructor(
    private val menuRepository: MenuRepository,
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(): Order? {
        val cartItems = menuRepository.getCart()
        if (cartItems.isEmpty()) return null

        val subtotal = menuRepository.getCartTotal()
        val tax = (subtotal * 0.05).toInt()
        val total = subtotal + tax

        val orderItems = cartItems.map {
            OrderItem(
                menuItemId = it.menuItem.id,
                menuItemName = it.menuItem.name,
                quantity = it.quantity,
                unitPrice = it.menuItem.price
            )
        }

        val order = Order(
            id = OrderId("ORD-${System.currentTimeMillis() % 10000}"),
            items = orderItems,
            subtotal = Price(subtotal),
            tax = Price(tax),
            totalAmount = Price(total)
        )

        orderRepository.placeOrder(order)
        menuRepository.clearCart()
        return order
    }
}

/**
 * SubmitFeedbackUseCase
 */
class SubmitFeedbackUseCase @Inject constructor(
    private val feedbackRepository: FeedbackRepository
) {
    suspend operator fun invoke(feedback: Feedback) {
        feedbackRepository.submitFeedback(feedback)
    }
}
