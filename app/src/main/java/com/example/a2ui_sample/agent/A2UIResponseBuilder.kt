package com.example.a2ui_sample.agent

import com.example.a2ui_sample.domain.model.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * A2UIResponseBuilder
 * Converts structured AgentResponse into A2UI JSON protocol.
 * Refactored to follow the structure provided by the user for better per-bubble rendering.
 */
class A2UIResponseBuilder {
    private val gson = Gson()
    private val surfaceId = "restaurant_surface"

    fun build(response: AgentResponse): List<String> {
        val messages = mutableListOf<String>()
        
        // 1. Ensure surface exists for every response bundle.
        messages.add(createSurfaceJson())

        // 2. Build the UI components based on the response type
        val updateJson = when (response) {
            is AgentResponse.MenuResults -> buildMenuResults(response.items, response.message)
            is AgentResponse.Recommendations -> buildMenuResults(response.items, "Recommendations")
            is AgentResponse.CartUpdate -> buildMessage("🛒 ${response.item.name} added to cart. You have ${response.totalCount} items.")
            is AgentResponse.CartView -> buildCartView(response.items, response.totalAmount)
            is AgentResponse.BookingConfirmation -> buildBookingConfirmation(response.booking)
            is AgentResponse.OrderConfirmation -> buildMessage("Order confirmed! ID: ${response.order.id.value}. Total: ₹${response.order.totalAmount.amount}")
            is AgentResponse.Error -> buildMessage("Error: ${response.message}")
            is AgentResponse.Message -> buildMessage(response.message)
            else -> buildMessage(response.toString())
        }
        messages.add(updateJson)
        
        return messages
    }

    private fun createSurfaceJson(): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val createSurface = JsonObject()
        createSurface.addProperty("surfaceId", surfaceId)
        createSurface.addProperty("catalogId", "restaurant")
        root.add("createSurface", createSurface)
        return gson.toJson(root)
    }

    private fun buildMenuResults(items: List<MenuItem>, titleText: String): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        
        val components = JsonArray()
        
        // Root Column
        val rootCol = JsonObject()
        rootCol.addProperty("id", "root")
        rootCol.addProperty("component", "Column")
        val children = JsonArray()
        children.add("header")
        items.forEachIndexed { index, item -> children.add("item_card_${item.id}_$index") }
        rootCol.add("children", children)
        components.add(rootCol)

        // Header
        val header = JsonObject()
        header.addProperty("id", "header")
        header.addProperty("component", "Text")
        header.addProperty("text", titleText)
        header.addProperty("variant", "h2")
        components.add(header)

        // Items
        items.forEachIndexed { index, item ->
            val uniqueId = "item_card_${item.id}_$index"
            val colId = "item_col_${item.id}_$index"
            
            val card = JsonObject()
            card.addProperty("id", uniqueId)
            card.addProperty("component", "Card")
            card.addProperty("child", colId)
            components.add(card)

            val col = JsonObject()
            col.addProperty("id", colId)
            col.addProperty("component", "Column")
            val colChildren = JsonArray()
            colChildren.add("item_img_${item.id}_$index")
            colChildren.add("item_name_${item.id}_$index")
            colChildren.add("item_price_${item.id}_$index")
            colChildren.add("item_btn_${item.id}_$index")
            col.add("children", colChildren)
            components.add(col)

            val img = JsonObject()
            img.addProperty("id", "item_img_${item.id}_$index")
            img.addProperty("component", "Image")
            img.addProperty("url", item.imageUrl)
            img.addProperty("variant", "smallFeature")
            components.add(img)

            val name = JsonObject()
            name.addProperty("id", "item_name_${item.id}_$index")
            name.addProperty("component", "Text")
            name.addProperty("text", item.name)
            name.addProperty("variant", "h4")
            components.add(name)

            val price = JsonObject()
            price.addProperty("id", "item_price_${item.id}_$index")
            price.addProperty("component", "Text")
            price.addProperty("text", "₹${item.price.amount}")
            price.addProperty("variant", "body")
            components.add(price)

            val btn = JsonObject()
            btn.addProperty("id", "item_btn_${item.id}_$index")
            btn.addProperty("component", "Button")
            btn.addProperty("text", "Add to Cart")
            btn.addProperty("variant", "primary")
            val action = JsonObject()
            val event = JsonObject()
            event.addProperty("name", "addToCart")
            val context = JsonObject()
            context.addProperty("itemId", item.id)
            event.add("context", context)
            action.add("event", event)
            btn.add("action", action)
            components.add(btn)
        }

        update.add("components", components)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    private fun buildCartView(items: List<CartItem>, totalAmount: Int): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        
        val components = JsonArray()
        val rootCol = JsonObject()
        rootCol.addProperty("id", "root")
        rootCol.addProperty("component", "Column")
        val children = JsonArray()
        children.add("cart_header")
        items.forEachIndexed { index, item -> children.add("cart_item_${item.menuItem.id}_$index") }
        children.add("cart_footer")
        rootCol.add("children", children)
        components.add(rootCol)

        val header = JsonObject()
        header.addProperty("id", "cart_header")
        header.addProperty("component", "Text")
        header.addProperty("text", "Your Shopping Cart")
        header.addProperty("variant", "h2")
        components.add(header)

        items.forEachIndexed { index, cartItem ->
            val itemText = JsonObject()
            itemText.addProperty("id", "cart_item_${cartItem.menuItem.id}_$index")
            itemText.addProperty("component", "Text")
            itemText.addProperty("text", "${cartItem.menuItem.name} x ${cartItem.quantity} = ₹${cartItem.menuItem.price.amount * cartItem.quantity}")
            itemText.addProperty("variant", "body")
            components.add(itemText)
        }

        val footer = JsonObject()
        footer.addProperty("id", "cart_footer")
        footer.addProperty("component", "Text")
        footer.addProperty("text", "Total Amount: ₹$totalAmount")
        footer.addProperty("variant", "h3")
        components.add(footer)

        update.add("components", components)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    private fun buildMessage(message: String): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        val comps = JsonArray()
        
        val text = JsonObject()
        text.addProperty("id", "root")
        text.addProperty("component", "Text")
        text.addProperty("text", message)
        text.addProperty("variant", "subtitle")
        comps.add(text)

        update.add("components", comps)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    private fun buildBookingConfirmation(booking: TableBooking): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        val components = JsonArray()

        val rootCol = JsonObject()
        rootCol.addProperty("id", "root")
        rootCol.addProperty("component", "Column")
        val children = JsonArray()
        children.add("booking_header")
        children.add("booking_id")
        children.add("booking_people")
        children.add("booking_time")
        rootCol.add("children", children)
        components.add(rootCol)

        val header = JsonObject()
        header.addProperty("id", "booking_header")
        header.addProperty("component", "Text")
        header.addProperty("text", "✓ Table Booking Confirmed")
        header.addProperty("variant", "h2")
        components.add(header)

        val bookingId = JsonObject()
        bookingId.addProperty("id", "booking_id")
        bookingId.addProperty("component", "Text")
        bookingId.addProperty("text", "Booking ID: ${booking.id}")
        bookingId.addProperty("variant", "subtitle")
        components.add(bookingId)

        val people = JsonObject()
        people.addProperty("id", "booking_people")
        people.addProperty("component", "Text")
        people.addProperty("text", "Number of People: ${booking.numberOfPeople}")
        people.addProperty("variant", "body")
        components.add(people)

        val time = JsonObject()
        time.addProperty("id", "booking_time")
        time.addProperty("component", "Text")
        time.addProperty("text", "Booking Time: ${booking.bookingDate} at ${booking.bookingTime}")
        time.addProperty("variant", "body")
        components.add(time)

        update.add("components", components)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }
}
