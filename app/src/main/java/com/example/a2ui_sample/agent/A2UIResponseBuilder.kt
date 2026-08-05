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

    /**
     * buildWithId
     * Version of build that uses a pre-generated unique surfaceId.
     */
    fun buildWithId(response: AgentResponse, surfaceId: String): List<String> {
        val messages = mutableListOf<String>()

        // 1. Initial Surface Creation with unique ID
        messages.add(createSurfaceJson(surfaceId))

        // 2. Build UI and Data based on response type
        when (response) {
            is AgentResponse.MenuResults -> {
                messages.add(buildMenuSchema(surfaceId, response.message))
                messages.add(buildMenuData(surfaceId, response.items))
            }
            is AgentResponse.Recommendations -> {
                messages.add(buildMenuSchema(surfaceId, response.message))
                messages.add(buildMenuData(surfaceId, response.items))
            }
            is AgentResponse.CartUpdate -> {
                messages.add(buildCartUpdateSchema(surfaceId, response.item, response.totalCount))
            }
            is AgentResponse.CartView -> {
                messages.add(buildCartViewSchema(surfaceId, response.totalAmount))
                messages.add(buildCartData(surfaceId, response.items))
            }
            is AgentResponse.BookingConfirmation -> {
                messages.add(buildBookingConfirmationSchema(surfaceId, response.booking))
            }
            is AgentResponse.BookingHistory -> {
                messages.add(buildBookingHistorySchema(surfaceId, response.message))
                messages.add(buildBookingData(surfaceId, response.bookings))
            }
            is AgentResponse.DeliveryUpdate -> {
                messages.add(buildDeliveryTrackingSchema(surfaceId, response.delivery, response.order))
            }
            is AgentResponse.FeedbackForm -> {
                messages.add(buildFeedbackFormSchema(surfaceId, response.orderId, response.message))
            }
            is AgentResponse.FeedbackSubmitted -> {
                messages.add(buildFeedbackConfirmationSchema(surfaceId, response.feedback, response.message))
            }
            is AgentResponse.FeedbackHistory -> {
                messages.add(buildFeedbackHistorySchema(surfaceId, response.message))
                messages.add(buildFeedbackData(surfaceId, response.feedbacks))
            }
            is AgentResponse.FeedbackDashboard -> {
                messages.add(buildFeedbackDashboardSchema(surfaceId, response.metrics, response.message))
            }
            is AgentResponse.OrderConfirmation -> {
                messages.add(buildSimpleMessage(surfaceId, "Order confirmed! ID: ${response.order.id.value}. Total: ₹${response.order.totalAmount.amount}"))
            }
            is AgentResponse.OrderSummary -> {
                messages.add(buildOrderSummarySchema(surfaceId, response.subtotal, response.tax, response.total))
                messages.add(buildCartData(surfaceId, response.items))
            }
            is AgentResponse.PaymentChoice -> {
                messages.add(buildPaymentChoiceSchema(surfaceId, response.total))
            }
            is AgentResponse.OrderPlaced -> {
                messages.add(buildOrderPlacedSchema(surfaceId, response.order))
            }
            is AgentResponse.FeedbackRequest -> {
                messages.add(buildFeedbackRequestSchema(surfaceId, response.orderId, response.prompt))
            }
            is AgentResponse.SatisfactionSurvey -> {
                messages.add(buildSatisfactionSchema(surfaceId, response.prompt, response.positiveText, response.negativeText))
            }
            is AgentResponse.Error -> {
                messages.add(buildSimpleMessage(surfaceId, "Error: ${response.message}"))
            }
            is AgentResponse.Message -> {
                messages.add(buildSimpleMessage(surfaceId, response.message))
            }
            else -> {
                messages.add(buildSimpleMessage(surfaceId, response.toString()))
            }
        }

        return messages
    }

    fun build(response: AgentResponse): List<String> {
        val uniqueId = "surf_${System.currentTimeMillis()}_${(0..999).random()}"
        return buildWithId(response, uniqueId)
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
            ch.add("divider-1")
            ch.add("subtotal-row")
            ch.add("tax-row")
            ch.add("divider-2")
            ch.add("total-row")
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

        // Cart Item Template: Row [Name x Qty, Price]
        comps.add(JsonObject().apply {
            addProperty("id", "cart-item-row")
            addProperty("component", "Row")
            val ch = JsonArray()
            ch.add("item-name-qty")
            ch.add("item-price")
            add("children", ch)
            addProperty("justify", "spaceBetween")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "item-name-qty")
            addProperty("component", "Text")
            val text = JsonObject()
            text.addProperty("template", "{quantity}x {menuItem/name}")
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
            addProperty("id", "divider-1")
            addProperty("component", "Divider")
        })

        // Subtotal Row
        comps.add(JsonObject().apply {
            addProperty("id", "subtotal-row")
            addProperty("component", "Row")
            val ch = JsonArray()
            ch.add("subtotal-label"); ch.add("subtotal-value")
            add("children", ch)
            addProperty("justify", "spaceBetween")
        })
        comps.add(JsonObject().apply { addProperty("id", "subtotal-label"); addProperty("component", "Text"); addProperty("text", "Subtotal"); addProperty("variant", "body") })
        comps.add(JsonObject().apply { 
            addProperty("id", "subtotal-value")
            addProperty("component", "Text")
            // We'll calculate tax manually for now in builder, but ideally data model should have it
            addProperty("text", "₹${(totalAmount / 1.05).toInt()}") 
            addProperty("variant", "body")
        })

        // Tax Row
        comps.add(JsonObject().apply {
            addProperty("id", "tax-row")
            addProperty("component", "Row")
            val ch = JsonArray()
            ch.add("tax-label"); ch.add("tax-value")
            add("children", ch)
            addProperty("justify", "spaceBetween")
        })
        comps.add(JsonObject().apply { addProperty("id", "tax-label"); addProperty("component", "Text"); addProperty("text", "GST (5%)"); addProperty("variant", "body") })
        comps.add(JsonObject().apply { 
            addProperty("id", "tax-value")
            addProperty("component", "Text")
            addProperty("text", "₹${(totalAmount - (totalAmount / 1.05)).toInt()}")
            addProperty("variant", "body")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "divider-2")
            addProperty("component", "Divider")
        })

        // Grand Total Row
        comps.add(JsonObject().apply {
            addProperty("id", "total-row")
            addProperty("component", "Row")
            val ch = JsonArray()
            ch.add("total-label"); ch.add("total-value")
            add("children", ch)
            addProperty("justify", "spaceBetween")
        })
        comps.add(JsonObject().apply { addProperty("id", "total-label"); addProperty("component", "Text"); addProperty("text", "Grand Total"); addProperty("variant", "h4") })
        comps.add(JsonObject().apply { addProperty("id", "total-value"); addProperty("component", "Text"); addProperty("text", "₹$totalAmount"); addProperty("variant", "h4") })

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
            addProperty("component", "Card")
            addProperty("child", "booking-col")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "booking-col")
            addProperty("component", "Column")
            val ch = JsonArray()
            ch.add("header-row")
            ch.add("divider")
            ch.add("booking-id")
            ch.add("guest-row")
            ch.add("date-row")
            ch.add("time-row")
            ch.add("restaurant-name")
            add("children", ch)
            addProperty("padding", 16)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "header-row")
            addProperty("component", "Row")
            val ch = JsonArray()
            ch.add("check-icon")
            ch.add("header-text")
            add("children", ch)
            addProperty("align", "center")
        })

        comps.add(JsonObject().apply { addProperty("id", "check-icon"); addProperty("component", "Icon"); addProperty("name", "check_circle"); addProperty("tint", "#4CAF50") })
        comps.add(JsonObject().apply { addProperty("id", "header-text"); addProperty("component", "Text"); addProperty("text", " Booking Confirmed"); addProperty("variant", "h5"); addProperty("color", "#4CAF50") })

        comps.add(JsonObject().apply { addProperty("id", "divider"); addProperty("component", "Divider") })

        comps.add(JsonObject().apply { addProperty("id", "booking-id"); addProperty("component", "Text"); addProperty("text", "ID: ${booking.id}"); addProperty("variant", "caption") })

        // Rows for Guest, Date, Time
        listOf(
            Triple("guest-row", "group", "${booking.numberOfPeople} Guests"),
            Triple("date-row", "event", booking.bookingDate),
            Triple("time-row", "schedule", booking.bookingTime)
        ).forEach { (id, icon, text) ->
            comps.add(JsonObject().apply {
                addProperty("id", id)
                addProperty("component", "Row")
                val ch = JsonArray()
                ch.add("$id-icon"); ch.add("$id-text")
                add("children", ch)
                addProperty("align", "center")
            })
            comps.add(JsonObject().apply { addProperty("id", "$id-icon"); addProperty("component", "Icon"); addProperty("name", icon); addProperty("size", 16) })
            comps.add(JsonObject().apply { addProperty("id", "$id-text"); addProperty("component", "Text"); addProperty("text", " $text"); addProperty("variant", "body") })
        }

        comps.add(JsonObject().apply { addProperty("id", "restaurant-name"); addProperty("component", "Text"); addProperty("text", "Luxe Dining Restaurant"); addProperty("variant", "subtitle"); addProperty("padding", 8) })

        update.add("components", comps)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    private fun buildBookingData(surfaceId: String, bookings: List<Reservation>): String {
        val uiBookings = bookings.map { b ->
            val sdf = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.US)
            mapOf(
                "restaurantName" to b.restaurantName,
                "status" to b.status.name,
                "displayTime" to sdf.format(java.util.Date(b.timeSlot.startMillis)),
                "guests" to "${b.partySize} People"
            )
        }
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        update.addProperty("path", "/bookings")
        update.add("value", gson.toJsonTree(uiBookings))
        root.add("updateDataModel", update)
        return gson.toJson(root)
    }

    private fun buildBookingHistorySchema(surfaceId: String, title: String): String {
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
            ch.add("booking-list")
            add("children", ch)
            addProperty("align", "stretch")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "header")
            addProperty("component", "Text")
            addProperty("text", title)
            addProperty("variant", "h5")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "booking-list")
            addProperty("component", "List")
            addProperty("direction", "vertical")
            val ch = JsonObject()
            ch.addProperty("path", "/bookings")
            ch.addProperty("componentId", "booking-card")
            add("children", ch)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "booking-card")
            addProperty("component", "Card")
            addProperty("child", "booking-row")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "booking-row")
            addProperty("component", "Row")
            val ch = JsonArray()
            ch.add("booking-details")
            ch.add("booking-status")
            add("children", ch)
            addProperty("justify", "spaceBetween")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "booking-details")
            addProperty("component", "Column")
            val ch = JsonArray()
            ch.add("rest-name")
            ch.add("booking-time")
            add("children", ch)
        })

        comps.add(JsonObject().apply { 
            addProperty("id", "rest-name")
            addProperty("component", "Text")
            val t = JsonObject(); t.addProperty("path", "restaurantName"); add("text", t)
            addProperty("variant", "subtitle") 
        })
        
        comps.add(JsonObject().apply { 
            addProperty("id", "booking-time")
            addProperty("component", "Text")
            val t = JsonObject(); t.addProperty("template", "{displayTime} • {guests}"); add("text", t)
            addProperty("variant", "caption") 
        })

        comps.add(JsonObject().apply { 
            addProperty("id", "booking-status")
            addProperty("component", "Text")
            val t = JsonObject(); t.addProperty("path", "status"); add("text", t)
            addProperty("variant", "body")
            addProperty("color", "#4CAF50")
        })

        update.add("components", comps)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    /**
     * DELIVERY TRACKING
     */
    private fun buildDeliveryTrackingSchema(surfaceId: String, delivery: Delivery, order: Order): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        val comps = JsonArray()

        // Root: Column [Card]
        comps.add(JsonObject().apply {
            addProperty("id", "root")
            addProperty("component", "Card")
            addProperty("child", "delivery-col")
        })

        // Content Column
        comps.add(JsonObject().apply {
            addProperty("id", "delivery-col")
            addProperty("component", "Column")
            val ch = JsonArray()
            ch.add("title")
            ch.add("status-row")
            ch.add("eta-text")
            ch.add("divider")
            ch.add("courier-row")
            ch.add("address-label")
            ch.add("address-text")
            add("children", ch)
            addProperty("padding", 16)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "title")
            addProperty("component", "Text")
            addProperty("text", "Track Order: ${order.id.value}")
            addProperty("variant", "h5")
        })

        // Status Row
        comps.add(JsonObject().apply {
            addProperty("id", "status-row")
            addProperty("component", "Row")
            val ch = JsonArray()
            ch.add("status-icon")
            ch.add("status-text")
            add("children", ch)
            addProperty("align", "center")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "status-icon")
            addProperty("component", "Icon")
            addProperty("name", "local_shipping")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "status-text")
            addProperty("component", "Text")
            addProperty("text", delivery.status.name.replace("_", " "))
            addProperty("variant", "h4")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "eta-text")
            addProperty("component", "Text")
            val eta = delivery.estimatedArrivalAt?.let { 
                java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(it))
            } ?: "Updating..."
            addProperty("text", "Estimated Arrival: $eta")
            addProperty("variant", "body")
        })

        comps.add(JsonObject().apply { addProperty("id", "divider"); addProperty("component", "Divider") })

        // Courier Row
        comps.add(JsonObject().apply {
            addProperty("id", "courier-row")
            addProperty("component", "Row")
            val ch = JsonArray()
            ch.add("courier-info")
            ch.add("call-button")
            add("children", ch)
            addProperty("justify", "spaceBetween")
            addProperty("align", "center")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "courier-info")
            addProperty("component", "Column")
            val ch = JsonArray()
            ch.add("courier-label")
            ch.add("courier-name")
            add("children", ch)
        })

        comps.add(JsonObject().apply { addProperty("id", "courier-label"); addProperty("component", "Text"); addProperty("text", "Delivery Partner"); addProperty("variant", "caption") })
        comps.add(JsonObject().apply { addProperty("id", "courier-name"); addProperty("component", "Text"); addProperty("text", delivery.courierName ?: "Assigning..."); addProperty("variant", "body") })

        comps.add(JsonObject().apply {
            addProperty("id", "call-button")
            addProperty("component", "Button")
            addProperty("text", "Call")
            addProperty("variant", "primary")
            // Note: Add call action if needed
        })

        comps.add(JsonObject().apply { addProperty("id", "address-label"); addProperty("component", "Text"); addProperty("text", "Delivering to:"); addProperty("variant", "caption") })
        comps.add(JsonObject().apply { addProperty("id", "address-text"); addProperty("component", "Text"); addProperty("text", delivery.deliveryAddress); addProperty("variant", "body") })

        update.add("components", comps)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    /**
     * FEEDBACK AGENT SCREENS
     */

    private fun buildFeedbackFormSchema(surfaceId: String, orderId: String, message: String): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        val comps = JsonArray()

        comps.add(JsonObject().apply {
            addProperty("id", "root")
            addProperty("component", "Card")
            addProperty("child", "form-col")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "form-col")
            addProperty("component", "Column")
            val ch = JsonArray()
            ch.add("title")
            ch.add("order-info")
            ch.add("rating-label")
            ch.add("rating-input")
            ch.add("comment-label")
            ch.add("comment-input")
            ch.add("submit-btn")
            add("children", ch)
            addProperty("padding", 16)
        })

        comps.add(JsonObject().apply { addProperty("id", "title"); addProperty("component", "Text"); addProperty("text", "Give Feedback"); addProperty("variant", "h5") })
        comps.add(JsonObject().apply { addProperty("id", "order-info"); addProperty("component", "Text"); addProperty("text", "Order: $orderId"); addProperty("variant", "caption") })
        
        comps.add(JsonObject().apply { addProperty("id", "rating-label"); addProperty("component", "Text"); addProperty("text", "How was the food?"); addProperty("variant", "body") })
        comps.add(JsonObject().apply { 
            addProperty("id", "rating-input")
            addProperty("component", "Slider") // Mocking a rating input
            addProperty("min", 1); addProperty("max", 5)
        })

        comps.add(JsonObject().apply { addProperty("id", "comment-label"); addProperty("component", "Text"); addProperty("text", "Any comments?"); addProperty("variant", "body") })
        comps.add(JsonObject().apply { 
            addProperty("id", "comment-input")
            addProperty("component", "TextField")
            addProperty("placeholder", "Share your experience...")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "submit-btn")
            addProperty("component", "Button")
            addProperty("text", "Submit Feedback")
            addProperty("variant", "primary")
            val action = JsonObject()
            val event = JsonObject()
            event.addProperty("name", "submitFeedback")
            val ctx = JsonObject()
            ctx.addProperty("orderId", orderId)
            event.add("context", ctx)
            action.add("event", event)
            add("action", action)
        })

        update.add("components", comps)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    private fun buildFeedbackConfirmationSchema(surfaceId: String, feedback: Feedback, message: String): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        val comps = JsonArray()

        comps.add(JsonObject().apply {
            addProperty("id", "root")
            addProperty("component", "Column")
            val ch = JsonArray()
            ch.add("icon")
            ch.add("msg")
            ch.add("rating-summary")
            add("children", ch)
            addProperty("align", "center")
        })

        comps.add(JsonObject().apply { addProperty("id", "icon"); addProperty("component", "Icon"); addProperty("name", "check_circle"); addProperty("variant", "h1") })
        comps.add(JsonObject().apply { addProperty("id", "msg"); addProperty("component", "Text"); addProperty("text", message); addProperty("variant", "h4") })
        comps.add(JsonObject().apply { addProperty("id", "rating-summary"); addProperty("component", "Text"); addProperty("text", "You rated this order ${feedback.overallRating.value}/5"); addProperty("variant", "body") })

        update.add("components", comps)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    private fun buildFeedbackHistorySchema(surfaceId: String, title: String): String {
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
            ch.add("fb-list")
            add("children", ch)
            addProperty("align", "stretch")
        })

        comps.add(JsonObject().apply { addProperty("id", "header"); addProperty("component", "Text"); addProperty("text", title); addProperty("variant", "h5") })
        
        comps.add(JsonObject().apply {
            addProperty("id", "fb-list")
            addProperty("component", "List")
            addProperty("direction", "vertical")
            val ch = JsonObject()
            ch.addProperty("path", "/feedbacks")
            ch.addProperty("componentId", "fb-card")
            add("children", ch)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "fb-card")
            addProperty("component", "Card")
            addProperty("child", "fb-row")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "fb-row")
            addProperty("component", "Row")
            val ch = JsonArray()
            ch.add("fb-details")
            ch.add("fb-rating")
            add("children", ch)
            addProperty("justify", "spaceBetween")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "fb-details")
            addProperty("component", "Column")
            val ch = JsonArray()
            ch.add("fb-order")
            ch.add("fb-comment")
            add("children", ch)
        })

        comps.add(JsonObject().apply { addProperty("id", "fb-order"); addProperty("component", "Text"); val t = JsonObject(); t.addProperty("path", "orderId/value"); add("text", t); addProperty("variant", "caption") })
        comps.add(JsonObject().apply { addProperty("id", "fb-comment"); addProperty("component", "Text"); val t = JsonObject(); t.addProperty("path", "comment"); add("text", t); addProperty("variant", "body") })
        comps.add(JsonObject().apply { addProperty("id", "fb-rating"); addProperty("component", "Text"); val t = JsonObject(); t.addProperty("path", "overallRating/value"); add("text", t); addProperty("variant", "h5") })

        update.add("components", comps)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    private fun buildFeedbackData(surfaceId: String, feedbacks: List<Feedback>): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        update.addProperty("path", "/feedbacks")
        update.add("value", gson.toJsonTree(feedbacks))
        root.add("updateDataModel", update)
        return gson.toJson(root)
    }

    private fun buildFeedbackDashboardSchema(surfaceId: String, metrics: FeedbackMetrics, title: String): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        val comps = JsonArray()

        comps.add(JsonObject().apply {
            addProperty("id", "root")
            addProperty("component", "Card")
            addProperty("child", "dash-col")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "dash-col")
            addProperty("component", "Column")
            val ch = JsonArray()
            ch.add("dash-title")
            ch.add("avg-row")
            ch.add("total-text")
            ch.add("sentiment-title")
            ch.add("sentiment-text")
            add("children", ch)
            addProperty("padding", 16)
        })

        comps.add(JsonObject().apply { addProperty("id", "dash-title"); addProperty("component", "Text"); addProperty("text", title); addProperty("variant", "h5") })
        
        comps.add(JsonObject().apply {
            addProperty("id", "avg-row")
            addProperty("component", "Row")
            val ch = JsonArray()
            ch.add("avg-label")
            ch.add("avg-value")
            add("children", ch)
            addProperty("align", "center")
        })
        
        comps.add(JsonObject().apply { addProperty("id", "avg-label"); addProperty("component", "Text"); addProperty("text", "Average Rating: "); addProperty("variant", "body") })
        comps.add(JsonObject().apply { addProperty("id", "avg-value"); addProperty("component", "Text"); addProperty("text", "%.1f".format(metrics.averageRating)); addProperty("variant", "h3") })
        
        comps.add(JsonObject().apply { addProperty("id", "total-text"); addProperty("component", "Text"); addProperty("text", "Based on ${metrics.totalReviews} reviews"); addProperty("variant", "caption") })
        
        comps.add(JsonObject().apply { addProperty("id", "sentiment-title"); addProperty("component", "Text"); addProperty("text", "Sentiment Analysis"); addProperty("variant", "subtitle") })
        
        val sentimentStr = metrics.sentimentSummary.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        comps.add(JsonObject().apply { addProperty("id", "sentiment-text"); addProperty("component", "Text"); addProperty("text", if (sentimentStr.isEmpty()) "No data" else sentimentStr); addProperty("variant", "body") })

        update.add("components", comps)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    /**
     * CHECKOUT FLOW
     */
    private fun buildOrderSummarySchema(surfaceId: String, subtotal: Int, tax: Int, total: Int): String {
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
            ch.add("divider-1")
            ch.add("subtotal-row")
            ch.add("tax-row")
            ch.add("divider-2")
            ch.add("total-row")
            ch.add("payment-prompt")
            ch.add("payment-actions")
            add("children", ch)
            addProperty("align", "stretch")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "title")
            addProperty("component", "Text")
            addProperty("text", "Order Summary")
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

        // Cart Item Template: Row [Name x Qty, Price]
        comps.add(JsonObject().apply {
            addProperty("id", "cart-item-row")
            addProperty("component", "Row")
            val ch = JsonArray()
            ch.add("item-name-qty")
            ch.add("item-price")
            add("children", ch)
            addProperty("justify", "spaceBetween")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "item-name-qty")
            addProperty("component", "Text")
            val text = JsonObject()
            text.addProperty("template", "{quantity}x {menuItem/name}")
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

        comps.add(JsonObject().apply { addProperty("id", "divider-1"); addProperty("component", "Divider") })

        // Subtotal
        comps.add(JsonObject().apply {
            addProperty("id", "subtotal-row")
            addProperty("component", "Row")
            val ch = JsonArray()
            ch.add("subtotal-label"); ch.add("subtotal-value")
            add("children", ch)
            addProperty("justify", "spaceBetween")
        })
        comps.add(JsonObject().apply { addProperty("id", "subtotal-label"); addProperty("component", "Text"); addProperty("text", "Subtotal"); addProperty("variant", "body") })
        comps.add(JsonObject().apply { addProperty("id", "subtotal-value"); addProperty("component", "Text"); addProperty("text", "₹$subtotal"); addProperty("variant", "body") })

        // Tax
        comps.add(JsonObject().apply {
            addProperty("id", "tax-row")
            addProperty("component", "Row")
            val ch = JsonArray()
            ch.add("tax-label"); ch.add("tax-value")
            add("children", ch)
            addProperty("justify", "spaceBetween")
        })
        comps.add(JsonObject().apply { addProperty("id", "tax-label"); addProperty("component", "Text"); addProperty("text", "GST (5%)"); addProperty("variant", "body") })
        comps.add(JsonObject().apply { addProperty("id", "tax-value"); addProperty("component", "Text"); addProperty("text", "₹$tax"); addProperty("variant", "body") })

        comps.add(JsonObject().apply { addProperty("id", "divider-2"); addProperty("component", "Divider") })

        // Grand Total
        comps.add(JsonObject().apply {
            addProperty("id", "total-row")
            addProperty("component", "Row")
            val ch = JsonArray()
            ch.add("total-label"); ch.add("total-value")
            add("children", ch)
            addProperty("justify", "spaceBetween")
        })
        comps.add(JsonObject().apply { addProperty("id", "total-label"); addProperty("component", "Text"); addProperty("text", "Grand Total"); addProperty("variant", "h4") })
        comps.add(JsonObject().apply { addProperty("id", "total-value"); addProperty("component", "Text"); addProperty("text", "₹$total"); addProperty("variant", "h4") })

        comps.add(JsonObject().apply {
            addProperty("id", "payment-prompt")
            addProperty("component", "Text")
            addProperty("text", "How would you like to pay?")
            addProperty("variant", "subtitle")
            addProperty("padding", 16)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "payment-actions")
            addProperty("component", "Column")
            val ch = JsonArray()
            ch.add("pay-now-btn")
            ch.add("pay-later-btn")
            add("children", ch)
            addProperty("align", "stretch")
            addProperty("spacing", 8)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "pay-now-btn")
            addProperty("component", "Button")
            addProperty("text", "💳 Pay Now")
            addProperty("variant", "primary")
            val action = JsonObject()
            val event = JsonObject()
            event.addProperty("name", "payNow")
            val ctx = JsonObject()
            ctx.addProperty("amount", total)
            event.add("context", ctx)
            action.add("event", event)
            add("action", action)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "pay-later-btn")
            addProperty("component", "Button")
            addProperty("text", "💵 COD")
            addProperty("variant", "secondary")
            val action = JsonObject()
            val event = JsonObject()
            event.addProperty("name", "payLater")
            val ctx = JsonObject()
            ctx.addProperty("amount", total)
            event.add("context", ctx)
            action.add("event", event)
            add("action", action)
        })

        update.add("components", comps)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    private fun buildPaymentChoiceSchema(surfaceId: String, total: Int): String {
        // Reuse same logic as summary or simpler
        return buildSimpleMessage(surfaceId, "Payment choice for ₹$total")
    }

    private fun buildOrderPlacedSchema(surfaceId: String, order: Order): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        val comps = JsonArray()

        comps.add(JsonObject().apply {
            addProperty("id", "root")
            addProperty("component", "Card")
            addProperty("child", "content-col")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "content-col")
            addProperty("component", "Column")
            val ch = JsonArray()
            ch.add("success-icon")
            ch.add("title")
            ch.add("order-id")
            ch.add("divider-top")
            ch.add("items-list")
            ch.add("divider-bot")
            ch.add("amount-row")
            ch.add("track-btn")
            add("children", ch)
            addProperty("align", "center")
            addProperty("padding", 20)
            addProperty("spacing", 8)
        })

        comps.add(JsonObject().apply { addProperty("id", "success-icon"); addProperty("component", "Icon"); addProperty("name", "check_circle"); addProperty("size", 48); addProperty("tint", "#4CAF50") })
        comps.add(JsonObject().apply { addProperty("id", "title"); addProperty("component", "Text"); addProperty("text", "Order Placed Successfully!"); addProperty("variant", "h5"); addProperty("color", "#4CAF50") })
        comps.add(JsonObject().apply { addProperty("id", "order-id"); addProperty("component", "Text"); addProperty("text", "ID: ${order.id.value}"); addProperty("variant", "caption") })
        
        comps.add(JsonObject().apply { addProperty("id", "divider-top"); addProperty("component", "Divider") })

        comps.add(JsonObject().apply {
            addProperty("id", "items-list")
            addProperty("component", "Column")
            val ch = JsonArray()
            order.items.forEachIndexed { index, item ->
                val itemId = "order-item-$index"
                comps.add(JsonObject().apply {
                    addProperty("id", itemId)
                    addProperty("component", "Text")
                    addProperty("text", "• ${item.quantity}x ${item.menuItemName}")
                    addProperty("variant", "body")
                })
                ch.add(itemId)
            }
            add("children", ch)
            addProperty("align", "start")
        })

        comps.add(JsonObject().apply { addProperty("id", "divider-bot"); addProperty("component", "Divider") })

        comps.add(JsonObject().apply {
            addProperty("id", "amount-row")
            addProperty("component", "Row")
            val ch = JsonArray(); ch.add("total-label"); ch.add("total-val")
            add("children", ch)
            addProperty("justify", "spaceBetween")
        })
        comps.add(JsonObject().apply { addProperty("id", "total-label"); addProperty("component", "Text"); addProperty("text", "Paid via COD"); addProperty("variant", "subtitle") })
        comps.add(JsonObject().apply { addProperty("id", "total-val"); addProperty("component", "Text"); addProperty("text", "₹${order.totalAmount.amount}"); addProperty("variant", "h5") })

        comps.add(JsonObject().apply {
            addProperty("id", "track-btn")
            addProperty("component", "Button")
            addProperty("text", "Track Order")
            addProperty("variant", "primary")
            val action = JsonObject()
            val event = JsonObject()
            event.addProperty("name", "trackOrder")
            val ctx = JsonObject()
            ctx.addProperty("orderId", order.id.value)
            event.add("context", ctx)
            action.add("event", event)
            add("action", action)
        })

        update.add("components", comps)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    private fun buildFeedbackRequestSchema(surfaceId: String, orderId: String, prompt: String): String {
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
            ch.add("header-row")
            ch.add("divider-top")
            ch.add("prompt-txt")
            ch.add("rating-list")
            ch.add("comment-box")
            ch.add("submit-btn")
            add("children", ch)
            addProperty("padding", 16)
            addProperty("align", "stretch")
            addProperty("spacing", 12)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "header-row")
            addProperty("component", "Row")
            val ch = JsonArray(); ch.add("star-icon"); ch.add("title-txt")
            add("children", ch)
            addProperty("align", "center")
        })
        comps.add(JsonObject().apply { addProperty("id", "star-icon"); addProperty("component", "Icon") ; addProperty("name", "star"); addProperty("tint", "#FFD700"); addProperty("size", 24) })
        comps.add(JsonObject().apply { addProperty("id", "title-txt"); addProperty("component", "Text"); addProperty("text", " Rate Your Experience"); addProperty("variant", "h5") })

        comps.add(JsonObject().apply { addProperty("id", "divider-top"); addProperty("component", "Divider") })

        comps.add(JsonObject().apply {
            addProperty("id", "prompt-txt")
            addProperty("component", "Text")
            addProperty("text", prompt)
            addProperty("variant", "body")
            addProperty("color", "#666666")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "rating-list")
            addProperty("component", "Column")
            val ch = JsonArray()
            ch.add("btn-excellent"); ch.add("btn-good"); ch.add("btn-average"); ch.add("btn-needs-improvement")
            add("children", ch)
            addProperty("spacing", 8)
        })

        fun createRatingBtn(id: String, text: String, rating: Int) {
            comps.add(JsonObject().apply {
                addProperty("id", id)
                addProperty("component", "Button")
                addProperty("text", text)
                addProperty("variant", "secondary")
                val action = JsonObject()
                val event = JsonObject()
                event.addProperty("name", "selectRating")
                val ctx = JsonObject()
                ctx.addProperty("rating", rating)
                ctx.addProperty("label", text)
                event.add("context", ctx)
                action.add("event", event)
                add("action", action)
            })
        }

        createRatingBtn("btn-excellent", "Excellent 🤩", 5)
        createRatingBtn("btn-good", "Good 🙂", 4)
        createRatingBtn("btn-average", "Average 😐", 3)
        createRatingBtn("btn-needs-improvement", "Needs Improvement 😕", 2)

        comps.add(JsonObject().apply {
            addProperty("id", "comment-box")
            addProperty("component", "TextField")
            addProperty("label", "Add a comment (Optional)")
            addProperty("placeholder", "How can we improve?")
            val value = JsonObject()
            value.addProperty("path", "/comment")
            add("value", value)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "submit-btn")
            addProperty("component", "Button")
            addProperty("text", "Submit Feedback")
            addProperty("variant", "primary")
            val action = JsonObject()
            val event = JsonObject()
            event.addProperty("name", "submit_premium_feedback")
            val ctx = JsonObject()
            ctx.addProperty("orderId", orderId)
            event.add("context", ctx)
            action.add("event", event)
            add("action", action)
        })

        update.add("components", comps)
        root.add("updateComponents", update)
        
        // Initial data model for comment
        val rootData = JsonObject()
        rootData.addProperty("version", "v0.10")
        val dataUpdate = JsonObject()
        dataUpdate.addProperty("surfaceId", surfaceId)
        dataUpdate.addProperty("path", "/comment")
        dataUpdate.addProperty("value", "")
        rootData.add("updateDataModel", dataUpdate)
        
        return gson.toJson(root) + "\n" + gson.toJson(rootData)
    }

    private fun buildSatisfactionSchema(surfaceId: String, prompt: String, positiveText: String, negativeText: String): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        val comps = JsonArray()

        comps.add(JsonObject().apply {
            addProperty("id", "root")
            addProperty("component", "Card")
            addProperty("child", "content-col")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "content-col")
            addProperty("component", "Column")
            val ch = JsonArray()
            ch.add("prompt-text")
            ch.add("actions")
            add("children", ch)
            addProperty("padding", 12)
            addProperty("align", "stretch")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "prompt-text")
            addProperty("component", "Text")
            addProperty("text", prompt)
            addProperty("variant", "body")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "actions")
            addProperty("component", "Row")
            val ch = JsonArray()
            ch.add("pos-btn")
            ch.add("neg-btn")
            add("children", ch)
            addProperty("justify", "spaceAround")
            addProperty("padding", 8)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "pos-btn")
            addProperty("component", "Button")
            addProperty("text", positiveText)
            addProperty("variant", "text")
            val action = JsonObject()
            val event = JsonObject()
            event.addProperty("name", "feedback_positive")
            action.add("event", event)
            add("action", action)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "neg-btn")
            addProperty("component", "Button")
            addProperty("text", negativeText)
            addProperty("variant", "text")
            val action = JsonObject()
            val event = JsonObject()
            event.addProperty("name", "feedback_negative")
            action.add("event", event)
            add("action", action)
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
