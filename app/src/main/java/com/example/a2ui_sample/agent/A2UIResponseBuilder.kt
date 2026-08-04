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
                messages.add(buildMenuSchema(surfaceId, "Recommendations for you"))
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
