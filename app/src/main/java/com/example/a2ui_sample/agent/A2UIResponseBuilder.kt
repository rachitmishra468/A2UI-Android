package com.example.a2ui_sample.agent

import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.MenuItem
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
            is AgentResponse.OrderPlaced -> buildOrderPlaced(response)
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

        // Root Container
        val rootColumn = JsonObject()
        rootColumn.addProperty("id", "root")
        rootColumn.addProperty("component", "Column")

        val rootChildren = JsonArray()
        rootChildren.add("header")

        items.forEach {
            rootChildren.add("card_${it.id}")
        }

        rootColumn.add("children", rootChildren)
        components.add(rootColumn)

        // Header
        val header = JsonObject()
        header.addProperty("id", "header")
        header.addProperty("component", "Text")
        header.addProperty("text", titleText)
        header.addProperty("variant", "h2")
        components.add(header)

        items.forEach { item ->
            val pid = item.id

            // Root Card for this item -> single flowing Column (no Tabs - cleaner in a narrow chat bubble)
            components.add(
                JsonObject().apply {
                    addProperty("id", "card_$pid")
                    addProperty("component", "Card")
                    addProperty("child", "card-col_$pid")
                }
            )

            components.add(
                JsonObject().apply {
                    addProperty("id", "card-col_$pid")
                    addProperty("component", "Column")
                    add(
                        "children",
                        JsonArray().apply {
                            add("recipe-image_$pid")
                            add("content-pad_$pid")
                        }
                    )
                }
            )

            // Image banner
            components.add(
                JsonObject().apply {
                    addProperty("id", "recipe-image_$pid")
                    addProperty("component", "Image")
                    addProperty("url", item.image)
                    addProperty("fit", "cover")
                }
            )

            // Padded content column
            components.add(
                JsonObject().apply {
                    addProperty("id", "content-pad_$pid")
                    addProperty("component", "Column")
                    add(
                        "children",
                        JsonArray().apply {
                            add("title_$pid")
                            add("rating-row_$pid")
                            add("times-text_$pid")
                            add("meta-divider1_$pid")
                            add("price-row_$pid")
                        }
                    )
                }
            )

            components.add(
                JsonObject().apply {
                    addProperty("id", "title_$pid")
                    addProperty("component", "Text")
                    addProperty("text", item.name)
                    addProperty("variant", "h3")
                }
            )

            // Rating row: star icon + rating + review count
            components.add(
                JsonObject().apply {
                    addProperty("id", "rating-row_$pid")
                    addProperty("component", "Row")
                    add("children", JsonArray().apply {
                        add("star-icon_$pid")
                        add("rating_$pid")
                        add("review-count_$pid")
                    })
                    addProperty("align", "center")
                }
            )

            components.add(
                JsonObject().apply {
                    addProperty("id", "star-icon_$pid")
                    addProperty("component", "Icon")
                    addProperty("name", "star")
                }
            )

            components.add(
                JsonObject().apply {
                    addProperty("id", "rating_$pid")
                    addProperty("component", "Text")
                    addProperty("text", item.rating)
                    addProperty("variant", "body")
                }
            )

            components.add(
                JsonObject().apply {
                    addProperty("id", "review-count_$pid")
                    addProperty("component", "Text")
                    val reviewWord = if (item.reviewCount == 1) "review" else "reviews"
                    addProperty("text", "(${item.reviewCount} $reviewWord)")
                    addProperty("variant", "caption")
                }
            )

            // Compact meta line: prep · cook · servings, all in one Text (no extra Rows/Icons/space)
            components.add(
                JsonObject().apply {
                    addProperty("id", "times-text_$pid")
                    addProperty("component", "Text")
                    val metaParts = listOfNotNull(
                        item.prepTime.takeIf { it.isNotBlank() },
                        item.cookTime.takeIf { it.isNotBlank() },
                        item.servings.takeIf { it.isNotBlank() }
                    )
                    addProperty("text", metaParts.joinToString("  •  "))
                    addProperty("variant", "caption")
                }
            )

            components.add(
                JsonObject().apply {
                    addProperty("id", "meta-divider1_$pid")
                    addProperty("component", "Divider")
                }
            )

            // Price + Add to Cart button, side by side
            components.add(
                JsonObject().apply {
                    addProperty("id", "price-row_$pid")
                    addProperty("component", "Row")
                    add("children", JsonArray().apply {
                        add("price_$pid")
                        add("btn_$pid")
                    })
                    addProperty("align", "center")
                    addProperty("justify", "spaceBetween")
                }
            )

            components.add(
                JsonObject().apply {
                    addProperty("id", "price_$pid")
                    addProperty("component", "Text")
                    addProperty("text", "₹${item.price}")
                    addProperty("variant", "h2")
                }
            )

            components.add(
                JsonObject().apply {
                    addProperty("id", "btn_text_$pid")
                    addProperty("component", "Text")
                    addProperty("text", "Add to Cart")
                }
            )

            components.add(
                JsonObject().apply {
                    addProperty("id", "btn_$pid")
                    addProperty("component", "Button")
                    addProperty("child", "btn_text_$pid")
                    addProperty("variant", "primary")

                    add(
                        "action",
                        JsonObject().apply {
                            add(
                                "event",
                                JsonObject().apply {
                                    addProperty("name", "addToCart")

                                    add(
                                        "context",
                                        JsonObject().apply {
                                            addProperty("itemId", item.id)
                                            addProperty("itemName", item.name)
                                            addProperty("price", item.price)
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
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

    private fun buildOrderPlaced(response: AgentResponse.OrderPlaced): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        val components = JsonArray()

        val rootCol = JsonObject()
        rootCol.addProperty("id", "root")
        rootCol.addProperty("component", "Column")
        val children = JsonArray()
        children.add("order_header")
        children.add("order_id")
        response.order.items.forEach { children.add("order_item_${it.menuItem.id}") }
        children.add("order_status")
        children.add("order_total")
        rootCol.add("children", children)
        components.add(rootCol)

        val header = JsonObject()
        header.addProperty("id", "order_header")
        header.addProperty("component", "Text")
        header.addProperty("text", "✓ Order Placed Successfully")
        header.addProperty("variant", "h2")
        components.add(header)

        val orderId = JsonObject()
        orderId.addProperty("id", "order_id")
        orderId.addProperty("component", "Text")
        orderId.addProperty("text", "Order ID: ${response.order.orderId}")
        orderId.addProperty("variant", "subtitle")
        components.add(orderId)

        response.order.items.forEach { orderItem ->
            val itemText = JsonObject()
            itemText.addProperty("id", "order_item_${orderItem.menuItem.id}")
            itemText.addProperty("component", "Text")
            itemText.addProperty("text", "${orderItem.menuItem.name} x ${orderItem.quantity}")
            components.add(itemText)
        }

        val status = JsonObject()
        status.addProperty("id", "order_status")
        status.addProperty("component", "Text")
        status.addProperty("text", "Status: ${response.order.status}")
        status.addProperty("variant", "body")
        components.add(status)

        val total = JsonObject()
        total.addProperty("id", "order_total")
        total.addProperty("component", "Text")
        total.addProperty("text", "Total Amount: ₹${response.order.totalAmount}")
        total.addProperty("variant", "h3")
        components.add(total)

        update.add("components", components)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }
}