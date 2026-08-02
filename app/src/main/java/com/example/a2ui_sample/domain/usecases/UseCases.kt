package com.example.a2ui_sample.domain.usecases

import com.example.a2ui_sample.domain.model.*
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.example.a2ui_sample.domain.valueobjects.OrderId
import com.example.a2ui_sample.domain.valueobjects.Price

interface SearchMenuUseCase {
    fun execute(query: String? = null, category: String? = null, type: String? = null, maxPrice: Int? = null): List<MenuItem>
}

class SearchMenuUseCaseImpl(private val repository: MenuRepository) : SearchMenuUseCase {
    override fun execute(query: String?, category: String?, type: String?, maxPrice: Int?): List<MenuItem> {
        return repository.searchMenu(category ?: query, type, maxPrice)
    }
}

interface AddToCartUseCase {
    fun execute(menuItemId: Int): AgentResponse.CartUpdate?
}

class AddToCartUseCaseImpl(private val repository: MenuRepository) : AddToCartUseCase {
    override fun execute(menuItemId: Int): AgentResponse.CartUpdate? {
        val cartItem = repository.addToCart(menuItemId) ?: return null
        return AgentResponse.CartUpdate(cartItem.menuItem, repository.getCartTotal())
    }
}

interface ViewCartUseCase {
    fun execute(): AgentResponse.CartView
}

class ViewCartUseCaseImpl(private val repository: MenuRepository) : ViewCartUseCase {
    override fun execute(): AgentResponse.CartView {
        return AgentResponse.CartView(repository.getCart(), repository.getCartTotal())
    }
}

interface BookTableUseCase {
    fun execute(numberOfPeople: Int, date: String, time: String): AgentResponse.BookingConfirmation
}

class BookTableUseCaseImpl(private val repository: MenuRepository) : BookTableUseCase {
    override fun execute(numberOfPeople: Int, date: String, time: String): AgentResponse.BookingConfirmation {
        val booking = TableBooking(numberOfPeople = numberOfPeople, bookingDate = date, bookingTime = time)
        repository.addBooking(booking)
        return AgentResponse.BookingConfirmation(booking)
    }
}

interface CalculatePriceUseCase {
    fun execute(): Int
}

class CalculatePriceUseCaseImpl(private val repository: MenuRepository) : CalculatePriceUseCase {
    override fun execute(): Int = repository.getCartTotal()
}

interface CheckoutUseCase {
    fun execute(): AgentResponse
}

class CheckoutUseCaseImpl(private val repository: MenuRepository) : CheckoutUseCase {
    override fun execute(): AgentResponse {
        val cartItems = repository.getCart()
        if (cartItems.isEmpty()) return AgentResponse.Error("Cart is empty")
        
        val subtotal = repository.getCartTotal()
        val tax = (subtotal * 0.05).toInt()
        val total = subtotal + tax
        
        val order = Order(
            id = OrderId("ORD-${System.currentTimeMillis() % 10000}"),
            items = cartItems.map { OrderItem(it.menuItem.id, it.menuItem.name, it.quantity, it.menuItem.price) },
            subtotal = Price(subtotal),
            tax = Price(tax),
            totalAmount = Price(total)
        )
        
        repository.placeOrder(order)
        return AgentResponse.OrderConfirmation(order)
    }
}
