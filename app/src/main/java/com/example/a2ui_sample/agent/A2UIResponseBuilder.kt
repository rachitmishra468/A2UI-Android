package com.example.a2ui_sample.agent

import com.example.a2ui_sample.domain.model.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * A2UIResponseBuilder
 * Converts AgentResponse into A2UI Protocol JSON.
 * Refactored to align with MenuA2UIBuilder modular architecture.
 */
class A2UIResponseBuilder {
    private val gson = Gson()

    fun build(response: AgentResponse): List<String> {
        val uniqueId = "surf_${System.currentTimeMillis()}_${(0..999).random()}"
        val messages = mutableListOf<String>()

        // 1. Initial Surface Creation
        messages.add(createSurfaceJson(uniqueId))

        // 2. Build UI and Data based on response type
        when (response) {
            is AgentResponse.MenuResults -> {
                messages.add(buildMenuSchema(uniqueId, response.message))
                messages.add(buildMenuData(uniqueId, response.items))
            }
            is AgentResponse.Recommendations -> {
                messages.add(buildMenuSchema(uniqueId, "Recommendations for you"))
                messages.add(buildMenuData(uniqueId, response.items))
            }
            is AgentResponse.CartUpdate -> {
                messages.add(buildCartUpdateSchema(uniqueId, response.item, response.totalCount))
            }
            is AgentResponse.CartView -> {
                messages.add(buildCartViewSchema(uniqueId, response.totalAmount))
                messages.add(buildCartData(uniqueId, response.items))
            }
            is AgentResponse.BookingConfirmation -> {
                messages.add(buildBookingConfirmationSchema(uniqueId, response.booking))
            }
            is AgentResponse.OrderConfirmation -> {
                messages.add(buildSimpleMessage(uniqueId, "Order confirmed! ID: ${response.order.id.value}. Total: ₹${response.order.totalAmount.amount}"))
            }
            is AgentResponse.Error -> {
                messages.add(buildSimpleMessage(uniqueId, "Error: ${response.message}"))
            }
            is AgentResponse.Message -> {
                messages.add(buildSimpleMessage(uniqueId, response.message))
            }
            else -> {
                messages.add(buildSimpleMessage(uniqueId, response.toString()))
            }
        }

        return messages
    }

    private fun createSurfaceJson(surfaceId: String): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val create = JsonObject()
        create.addProperty("surfaceId", surfaceId)
        create.addProperty("catalogId", "restaurant")
        root.add("createSurface", create)
        return gson.toJson(root)
    }

    /**
     * MENU RESULTS / RECOMMENDATIONS
     */

    private fun buildMenuSchema(surfaceId: String, title: String): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        val comps = JsonArray()

        // Root: Column [Header, List]
        comps.add(JsonObject().apply {
            addProperty("id", "root")
            addProperty("component", "Column")
            val ch = JsonArray()
            ch.add("header")
            ch.add("menu-list")
            add("children", ch)
            addProperty("align", "stretch")
        })

        // Header
        comps.add(JsonObject().apply {
            addProperty("id", "header")
            addProperty("component", "Text")
            addProperty("text", title)
            addProperty("variant", "h2")
        })

        // List
        comps.add(JsonObject().apply {
            addProperty("id", "menu-list")
            addProperty("component", "List")
            addProperty("direction", "vertical")
            val ch = JsonObject()
            ch.addProperty("path", "/items")
            ch.addProperty("componentId", "menu-card")
            add("children", ch)
        })

        // Card Template
        comps.add(JsonObject().apply {
            addProperty("id", "menu-card")
            addProperty("component", "Card")
            addProperty("child", "menu-item-col")
        })

        // Card Content: Column [Image, Name, Price, Button]
        comps.add(JsonObject().apply {
            addProperty("id", "menu-item-col")
            addProperty("component", "Column")
            val ch = JsonArray()
            ch.add("food-image")
            ch.add("food-name")
            ch.add("food-price")
            ch.add("add-button")
            add("children", ch)
            addProperty("padding", 8)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "food-image")
            addProperty("component", "Image")
            val url = JsonObject()
            url.addProperty("path", "image")
            add("url", url)
            addProperty("variant", "smallFeature")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "food-name")
            addProperty("component", "Text")
            val text = JsonObject()
            text.addProperty("path", "name")
            add("text", text)
            addProperty("variant", "h5")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "food-price")
            addProperty("component", "Text")
            val text = JsonObject()
            text.addProperty("path", "price/amount")
            add("text", text)
            addProperty("variant", "body")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "add-button")
            addProperty("component", "Button")
            addProperty("text", "Add to Cart")
            addProperty("variant", "primary")
            val action = JsonObject()
            val event = JsonObject()
            event.addProperty("name", "addToCart")
            val context = JsonObject()
            val itemId = JsonObject()
            itemId.addProperty("path", "id")
            context.add("itemId", itemId)
            event.add("context", context)
            action.add("event", event)
            add("action", action)
        })

        update.add("components", comps)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    private fun buildMenuData(surfaceId: String, items: List<MenuItem>): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        update.addProperty("path", "/items")
        update.add("value", gson.toJsonTree(items))
        root.add("updateDataModel", update)
        return gson.toJson(root)
    }

    /**
     * CART UPDATE
     */

    private fun buildCartUpdateSchema(surfaceId: String, item: MenuItem, totalCount: Int): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        val comps = JsonArray()

        comps.add(JsonObject().apply {
            addProperty("id", "root")
            addProperty("component", "Card")
            addProperty("child", "content")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "content")
            addProperty("component", "Column")
            val ch = JsonArray()
            ch.add("msg")
            ch.add("actions")
            add("children", ch)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "msg")
            addProperty("component", "Text")
            addProperty("text", "🛒 ${item.name} added. Total items: $totalCount")
            addProperty("variant", "body")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "actions")
            addProperty("component", "Row")
            val ch = JsonArray()
            ch.add("view-cart")
            ch.add("checkout")
            add("children", ch)
            addProperty("justify", "spaceAround")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "view-cart")
            addProperty("component", "Button")
            addProperty("text", "View Cart")
            addProperty("variant", "borderless")
            val action = JsonObject()
            val event = JsonObject()
            event.addProperty("name", "viewCart")
            action.add("event", event)
            add("action", action)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "checkout")
            addProperty("component", "Button")
            addProperty("text", "Checkout")
            addProperty("variant", "primary")
            val action = JsonObject()
            val event = JsonObject()
            event.addProperty("name", "checkout")
            action.add("event", event)
            add("action", action)
        })

        update.add("components", comps)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    /**
     * CART VIEW
     */

    private fun buildCartViewSchema(surfaceId: String, totalAmount: Int): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        val comps = JsonArray()

        comps.add(JsonObject().apply {
            addProperty("id", "root")
            addProperty("component", "Card")
            addProperty("child", "main-col")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "main-col")
            addProperty("component", "Column")
            val ch = JsonArray()
            ch.add("title")
            ch.add("cart-list")
            ch.add("divider")
            ch.add("total")
            ch.add("checkout-btn")
            add("children", ch)
            addProperty("align", "stretch")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "title")
            addProperty("component", "Text")
            addProperty("text", "Your Order Summary")
            addProperty("variant", "h5")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "cart-list")
            addProperty("component", "List")
            addProperty("direction", "vertical")
            val ch = JsonObject()
            ch.addProperty("path", "/cart-items")
            ch.addProperty("componentId", "cart-item-row")
            add("children", ch)
        })

        // Cart Item Template: Row [Name, Price]
        comps.add(JsonObject().apply {
            addProperty("id", "cart-item-row")
            addProperty("component", "Row")
            val ch = JsonArray()
            ch.add("item-name")
            ch.add("item-price")
            add("children", ch)
            addProperty("justify", "spaceBetween")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "item-name")
            addProperty("component", "Text")
            val text = JsonObject()
            text.addProperty("path", "menuItem/name")
            add("text", text)
            addProperty("variant", "body")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "item-price")
            addProperty("component", "Text")
            val text = JsonObject()
            text.addProperty("path", "menuItem/price/amount")
            add("text", text)
            addProperty("variant", "body")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "divider")
            addProperty("component", "Divider")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "total")
            addProperty("component", "Text")
            addProperty("text", "Total Amount: ₹$totalAmount")
            addProperty("variant", "h4")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "checkout-btn")
            addProperty("component", "Button")
            addProperty("text", "Proceed to Checkout")
            addProperty("variant", "primary")
            val action = JsonObject()
            val event = JsonObject()
            event.addProperty("name", "checkout")
            action.add("event", event)
            add("action", action)
        })

        update.add("components", comps)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    private fun buildCartData(surfaceId: String, items: List<CartItem>): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        update.addProperty("path", "/cart-items")
        update.add("value", gson.toJsonTree(items))
        root.add("updateDataModel", update)
        return gson.toJson(root)
    }

    /**
     * BOOKING CONFIRMATION
     */

    private fun buildBookingConfirmationSchema(surfaceId: String, booking: TableBooking): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        val comps = JsonArray()

        comps.add(JsonObject().apply {
            addProperty("id", "root")
            addProperty("component", "Column")
            val ch = JsonArray()
            ch.add("header")
            ch.add("booking-id")
            ch.add("booking-details")
            add("children", ch)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "header")
            addProperty("component", "Text")
            addProperty("text", "✓ Table Booking Confirmed")
            addProperty("variant", "h2")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "booking-id")
            addProperty("component", "Text")
            addProperty("text", "Booking ID: ${booking.id}")
            addProperty("variant", "subtitle")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "booking-details")
            addProperty("component", "Text")
            addProperty("text", "Table for ${booking.numberOfPeople} on ${booking.bookingDate} at ${booking.bookingTime}")
            addProperty("variant", "body")
        })

        update.add("components", comps)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    /**
     * SIMPLE MESSAGE
     */

    private fun buildSimpleMessage(surfaceId: String, message: String): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        val comps = JsonArray()
        
        comps.add(JsonObject().apply {
            addProperty("id", "root")
            addProperty("component", "Text")
            addProperty("text", message)
            addProperty("variant", "subtitle")
        })

        update.add("components", comps)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }
}
