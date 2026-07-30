package com.example.a2ui_sample.domain.usecases

import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.CartItem
import com.example.a2ui_sample.domain.model.MenuItem
import com.example.a2ui_sample.data.repository.MenuRepository
import com.example.a2ui_sample.domain.model.TableBooking

/**
 * Domain use-case interfaces and simple implementations that wrap MenuRepository.
 * These implement the core business logic so both manual UI and agents reuse them.
 */

interface SearchMenuUseCase {
    fun execute(query: String?, category: String?, type: String?, maxPrice: Int?): List<MenuItem>
}

class SearchMenuUseCaseImpl(private val repository: MenuRepository) : SearchMenuUseCase {
    override fun execute(query: String?, category: String?, type: String?, maxPrice: Int?): List<MenuItem> {
        // We use category as primary filter; if query present and category null, use query as category/name
        val cat = category ?: query
        return repository.searchMenu(cat, type, maxPrice)
    }
}

interface AddToCartUseCase {
    fun execute(menuItemId: Int): AgentResponse.CartUpdate?
}

class AddToCartUseCaseImpl(private val repository: MenuRepository) : AddToCartUseCase {
    override fun execute(menuItemId: Int): AgentResponse.CartUpdate? {
        val added = repository.addToCart(menuItemId) ?: return null
        val totalCount = repository.getCart().sumOf { it.quantity }
        return AgentResponse.CartUpdate(addedItem = added.menuItem, totalCount = totalCount)
    }
}

interface ViewCartUseCase {
    fun execute(): AgentResponse.CartView
}

class ViewCartUseCaseImpl(private val repository: MenuRepository) : ViewCartUseCase {
    override fun execute(): AgentResponse.CartView {
        val items = repository.getCart()
        val total = repository.getCartTotal()
        return AgentResponse.CartView(items, total)
    }
}

interface BookTableUseCase {
    fun execute(numberOfPeople: Int, bookingTime: String): AgentResponse.BookingConfirmation
}

class BookTableUseCaseImpl(private val repository: MenuRepository) : BookTableUseCase {
    override fun execute(numberOfPeople: Int, bookingTime: String): AgentResponse.BookingConfirmation {
        val booking = TableBooking(numberOfPeople = numberOfPeople, bookingTime = bookingTime)
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
    fun execute(customerId: String?): AgentResponse
}

class CheckoutUseCaseImpl(private val repository: MenuRepository) : CheckoutUseCase {
    override fun execute(customerId: String?): AgentResponse {
        val order = repository.placeOrder()
            ?: return AgentResponse.Error("Your cart is empty. Add items before checking out.")
        return AgentResponse.OrderPlaced(order)
    }
}

