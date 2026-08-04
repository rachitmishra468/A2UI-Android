package com.example.a2ui_sample.infrastructure.repository

import android.content.Context
import com.example.a2ui_sample.domain.model.*
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.example.a2ui_sample.domain.valueobjects.*
import com.example.a2ui_sample.infrastructure.persistence.dao.CartDao
import com.example.a2ui_sample.infrastructure.persistence.dao.MenuDao
import com.example.a2ui_sample.infrastructure.persistence.entity.CartEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cartDao: CartDao,
    private val menuDao: MenuDao
) : MenuRepository {
    private val gson = Gson()

    private val menuItemsCache: List<MenuItem> by lazy {
        try {
            val jsonString = context.assets.open("menu.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<MenuItem>>() {}.type
            gson.fromJson<List<MenuItem>>(jsonString, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getMenuItems(): List<MenuItem> = menuItemsCache

    override fun searchMenu(category: String?, type: String?, maxPrice: Int?): List<MenuItem> {
        return menuItemsCache.filter { item ->
            val matchesCategory = category == null || item.category.contains(category, ignoreCase = true) || item.name.contains(category, ignoreCase = true)
            val matchesType = type == null || item.type.name.equals(type, ignoreCase = true)
            val matchesPrice = maxPrice == null || item.price.amount <= maxPrice
            matchesCategory && matchesType && matchesPrice
        }
    }

    override suspend fun addToCart(menuItemId: Int): CartItem? = withContext(Dispatchers.IO) {
        val item = menuItemsCache.find { it.id == menuItemId } ?: return@withContext null
        val existing = cartDao.getCartItems().first().find { it.cartItemId == menuItemId }
        if (existing != null) {
            cartDao.updateCartItem(existing.copy(quantity = existing.quantity + 1))
        } else {
            cartDao.insertCartItem(CartEntity(
                cartItemId = item.id,
                itemName = item.name,
                quantity = 1,
                price = item.price.amount,
                imageUrl = item.image
            ))
        }
        return@withContext CartItem(item, (existing?.quantity ?: 0) + 1)
    }

    override suspend fun getCart(): List<CartItem> = withContext(Dispatchers.IO) {
        try {
            val entities = cartDao.getCartItems().first()
            entities.map { entity ->
                val menuItem = menuItemsCache.find { it.id == entity.cartItemId } ?: MenuItem(
                    id = entity.cartItemId,
                    name = entity.itemName,
                    description = "",
                    price = Price(entity.price),
                    category = "",
                    image = entity.imageUrl,
                    type = MenuItemType.VEG
                )
                CartItem(menuItem, entity.quantity)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getCartFlow(): Flow<List<CartItem>> {
        return cartDao.getCartItems().map { entities ->
            entities.map { entity ->
                val menuItem = menuItemsCache.find { it.id == entity.cartItemId } ?: MenuItem(
                    id = entity.cartItemId,
                    name = entity.itemName,
                    description = "",
                    price = Price(entity.price),
                    category = "",
                    image = entity.imageUrl,
                    type = MenuItemType.VEG
                )
                CartItem(menuItem, entity.quantity)
            }
        }
    }

    override suspend fun getCartTotal(): Int {
        return getCart().sumOf { it.quantity * it.menuItem.price.amount }
    }

    override suspend fun updateCartQuantity(menuItemId: Int, quantity: Int): CartItem? = withContext(Dispatchers.IO) {
        if (quantity <= 0) {
            cartDao.getCartItems().first().find { it.cartItemId == menuItemId }?.let {
                cartDao.deleteCartItem(it)
            }
        } else {
            val existing = cartDao.getCartItems().first().find { it.cartItemId == menuItemId }
            existing?.let {
                cartDao.updateCartItem(it.copy(quantity = quantity))
            }
        }
        return@withContext null
    }

    override suspend fun removeFromCart(menuItemId: Int): Boolean = withContext(Dispatchers.IO) {
        cartDao.getCartItems().first().find { it.cartItemId == menuItemId }?.let {
            cartDao.deleteCartItem(it)
            return@withContext true
        }
        return@withContext false
    }

    override suspend fun clearCart() = withContext(Dispatchers.IO) {
        cartDao.clearCart()
    }

    override suspend fun addBooking(booking: TableBooking) {
        // Handled in ReservationRepository
    }

    override suspend fun getBookings(): List<TableBooking> = emptyList()

    override suspend fun cancelBooking(bookingId: String): Boolean = true

    override suspend fun placeOrder(order: Order): Boolean {
        clearCart()
        return true
    }

    override suspend fun getCurrentOrders(): List<Order> = emptyList()

    override suspend fun getPastOrders(): List<Order> = emptyList()

    override suspend fun completeOrder(orderId: String) {}

    override suspend fun getDeliveryStatus(orderId: String): Delivery? = null
}
