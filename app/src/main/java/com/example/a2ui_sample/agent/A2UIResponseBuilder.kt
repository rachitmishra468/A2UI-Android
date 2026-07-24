package com.example.a2ui_sample.agent

import com.example.a2ui_sample.data.AgentResponse
import com.example.a2ui_sample.data.MenuItem
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * A2UIResponseBuilder
 * Converts structured AgentResponse into A2UI JSON protocol.
 */
class A2UIResponseBuilder {
    private val gson = Gson()
    private val surfaceId = "restaurant_surface"

    fun build(response: AgentResponse): List<String> {
        val messages = mutableListOf<String>()
        
        // 1. Ensure surface exists for every response bundle.
        // This allows independent rendering in chat bubbles without state bleeding.
        messages.add(createSurfaceJson())

        // 2. Build the UI components based on the response type
        val updateJson = when (response) {
            is AgentResponse.MenuResults -> buildMenuResults(response.items, response.query)
            is AgentResponse.Recommendations -> buildMenuResults(response.items, "Recommendations")
            is AgentResponse.CartUpdate -> buildMessage("Added ${response.addedItem.name} to cart. Total items: ${response.totalCount}")
            is AgentResponse.CartView -> buildCartView(response)
            is AgentResponse.BookingRequest -> buildMessage(
                when (response.step) {
                    "ask_people" -> "For how many people would you like to book a table?"
                    "ask_time" -> "What time would you like to book the table?"
                    else -> response.query
                }
            )
            is AgentResponse.BookingConfirmation -> buildBookingConfirmation(response)
            is AgentResponse.Error -> buildMessage("Error: ${response.message}")
            is AgentResponse.Message -> buildMessage(response.content)
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
        items.forEach { children.add("item_card_${it.id}") }
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
        items.forEach { item ->
            // Card wraps a Column which wraps Image and Text
            val card = JsonObject()
            card.addProperty("id", "item_card_${item.id}")
            card.addProperty("component", "Card")
            card.addProperty("child", "item_col_${item.id}")
            components.add(card)

            val col = JsonObject()
            col.addProperty("id", "item_col_${item.id}")
            col.addProperty("component", "Column")
            val colChildren = JsonArray()
            colChildren.add("item_img_${item.id}")
            colChildren.add("item_name_${item.id}")
            colChildren.add("item_price_${item.id}")
            col.add("children", colChildren)
            components.add(col)

            val img = JsonObject()
            img.addProperty("id", "item_img_${item.id}")
            img.addProperty("component", "Image")
            img.addProperty("url", item.image)
            img.addProperty("variant", "mediumFeature")
            components.add(img)

            val name = JsonObject()
            name.addProperty("id", "item_name_${item.id}")
            name.addProperty("component", "Text")
            name.addProperty("text", item.name)
            name.addProperty("variant", "h4")
            components.add(name)

            val price = JsonObject()
            price.addProperty("id", "item_price_${item.id}")
            price.addProperty("component", "Text")
            price.addProperty("text", "₹${item.price}")
            price.addProperty("variant", "body")
            components.add(price)
        }

        update.add("components", components)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    private fun buildCartView(response: AgentResponse.CartView): String {
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
        response.cartItems.forEach { children.add("cart_item_${it.menuItem.id}") }
        children.add("cart_footer")
        rootCol.add("children", children)
        components.add(rootCol)

        val header = JsonObject()
        header.addProperty("id", "cart_header")
        header.addProperty("component", "Text")
        header.addProperty("text", "Your Shopping Cart")
        header.addProperty("variant", "h2")
        components.add(header)

        response.cartItems.forEach { cartItem ->
            val itemText = JsonObject()
            itemText.addProperty("id", "cart_item_${cartItem.menuItem.id}")
            itemText.addProperty("component", "Text")
            itemText.addProperty("text", "${cartItem.menuItem.name} x ${cartItem.quantity} = ₹${cartItem.menuItem.price * cartItem.quantity}")
            components.add(itemText)
        }

        val footer = JsonObject()
        footer.addProperty("id", "cart_footer")
        footer.addProperty("component", "Text")
        footer.addProperty("text", "Total Amount: ₹${response.totalAmount}")
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
        
        // We use "root" here. This is now safe because each chat bubble has its own 
        // local renderer, so this root will only define the content for this specific bubble.
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

    private fun buildBookingConfirmation(response: AgentResponse.BookingConfirmation): String {
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
        bookingId.addProperty("text", "Booking ID: ${response.booking.bookingId}")
        bookingId.addProperty("variant", "subtitle")
        components.add(bookingId)

        val people = JsonObject()
        people.addProperty("id", "booking_people")
        people.addProperty("component", "Text")
        people.addProperty("text", "Number of People: ${response.booking.numberOfPeople}")
        people.addProperty("variant", "body")
        components.add(people)

        val time = JsonObject()
        time.addProperty("id", "booking_time")
        time.addProperty("component", "Text")
        time.addProperty("text", "Booking Time: ${response.booking.bookingTime}")
        time.addProperty("variant", "body")
        components.add(time)

        update.add("components", components)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }
}