package com.example.a2ui_sample.presentation.a2ui

import com.example.a2ui_sample.domain.model.MenuItem
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * HomeA2UIBuilder
 * Modular A2UI Builder for the Home Screen following the project's best practices.
 */
class HomeA2UIBuilder {
    private val gson = Gson()
    private val surfaceId = "home_screen_surface"

    fun createSurfaceJson(): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val createSurface = JsonObject()
        createSurface.addProperty("surfaceId", surfaceId)
        root.add("createSurface", createSurface)
        return gson.toJson(root)
    }

    /**
     * Component Definitions
     */

    private fun createHeroBanner(): JsonObject = JsonObject().apply {
        addProperty("id", "hero-banner")
        addProperty("component", "Card")
        addProperty("child", "hero-content")
    }

    private fun createHeroContent(): JsonObject = JsonObject().apply {
        addProperty("id", "hero-content")
        addProperty("component", "Box")
        add("children", JsonArray().apply { add("hero-image"); add("hero-text-overlay") })
    }

    private fun createHeroImage(): JsonObject = JsonObject().apply {
        addProperty("id", "hero-image")
        addProperty("component", "Image")
        addProperty("url", "https://images.unsplash.com/photo-1514362545857-3bc16c4c7d1b?q=80&w=1000")
        addProperty("variant", "largeFeature")
        addProperty("fit", "cover")
    }

    private fun createHeroTextOverlay(): JsonObject = JsonObject().apply {
        addProperty("id", "hero-text-overlay")
        addProperty("component", "Column")
        add("children", JsonArray().apply { add("hero-title"); add("hero-subtitle") })
        addProperty("padding", 20)
        addProperty("align", "start")
    }

    private fun createHeroTitle(): JsonObject = JsonObject().apply {
        addProperty("id", "hero-title")
        addProperty("component", "Text")
        addProperty("text", "Luxe Dining")
        addProperty("variant", "h1")
    }

    private fun createHeroSubtitle(): JsonObject = JsonObject().apply {
        addProperty("id", "hero-subtitle")
        addProperty("component", "Text")
        addProperty("text", "Experience Culinary Excellence")
        addProperty("variant", "h4")
    }

    private fun createQuickActions(): JsonObject = JsonObject().apply {
        addProperty("id", "quick-actions")
        addProperty("component", "Row")
        add("children", JsonArray().apply { 
            add("action-menu"); add("action-book"); add("action-chat"); add("action-offers") 
        })
        addProperty("justify", "spaceEvenly")
        addProperty("padding", 16)
    }

    private fun createActionItem(id: String, icon: String, label: String, eventName: String): JsonObject = JsonObject().apply {
        addProperty("id", id)
        addProperty("component", "Card")
        addProperty("child", "$id-col")
        addProperty("weight", 1)
        addProperty("margin", 4)
        
        val col = JsonObject()
        col.addProperty("id", "$id-col")
        col.addProperty("component", "Column")
        col.add("children", JsonArray().apply { add("$id-icon"); add("$id-text") })
        col.addProperty("align", "center")
        col.addProperty("padding", 8)
        
        val action = JsonObject()
        val event = JsonObject()
        event.addProperty("name", eventName)
        action.add("event", event)
        col.add("action", action)
        
        val iconComp = JsonObject()
        iconComp.addProperty("id", "$id-icon")
        iconComp.addProperty("component", "Icon")
        iconComp.addProperty("name", icon)
        
        val textComp = JsonObject()
        textComp.addProperty("id", "$id-text")
        textComp.addProperty("component", "Text")
        textComp.addProperty("text", label)
        textComp.addProperty("variant", "caption")
        
        // These need to be added to the components list separately, but for brevity in this helper:
        // (In a real implementation, we'd return a list or add to a registry)
    }

    // Since the reference used modular functions returning JsonObject, I'll stick to that but return them as a list for schema.
    
    fun buildHomeSchema(): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        val comps = JsonArray()

        // Root Layout
        comps.add(JsonObject().apply {
            addProperty("id", "root")
            addProperty("component", "Column")
            add("children", JsonArray().apply { 
                add("hero-banner"); add("quick-actions"); add("featured-title"); add("featured-list"); add("offer-card")
            })
            addProperty("align", "stretch")
        })

        // Hero
        comps.add(createHeroBanner())
        comps.add(createHeroContent())
        comps.add(createHeroImage())
        comps.add(createHeroTextOverlay())
        comps.add(createHeroTitle())
        comps.add(createHeroSubtitle())

        // Quick Actions
        comps.add(createQuickActions())
        addQuickActionComps(comps, "action-menu", "restaurantMenu", "Menu", "openMenu")
        addQuickActionComps(comps, "action-book", "event", "Book", "openBooking")
        addQuickActionComps(comps, "action-chat", "autoAwesome", "AI Chat", "openChat")
        addQuickActionComps(comps, "action-offers", "localOffer", "Offers", "openOffers")

        // Featured Section
        comps.add(JsonObject().apply {
            addProperty("id", "featured-title")
            addProperty("component", "Text")
            addProperty("text", "Chef's Specials")
            addProperty("variant", "h3")
            addProperty("padding", 16)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "featured-list")
            addProperty("component", "List")
            addProperty("direction", "horizontal")
            val children = JsonObject()
            children.addProperty("path", "/featured")
            children.addProperty("componentId", "featured-item-card")
            add("children", children)
        })

        // Featured Item Template
        comps.add(JsonObject().apply {
            addProperty("id", "featured-item-card")
            addProperty("component", "Card")
            addProperty("child", "fi-col")
            addProperty("width", 200)
            addProperty("margin", 8)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "fi-col")
            addProperty("component", "Column")
            add("children", JsonArray().apply { add("fi-img"); add("fi-name"); add("fi-price"); add("fi-btn") })
            addProperty("padding", 8)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "fi-img")
            addProperty("component", "Image")
            val url = JsonObject()
            url.addProperty("path", "image")
            add("url", url)
            addProperty("variant", "smallFeature")
            addProperty("height", 120)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "fi-name")
            addProperty("component", "Text")
            val text = JsonObject()
            text.addProperty("path", "name")
            add("text", text)
            addProperty("variant", "h5")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "fi-price")
            addProperty("component", "Text")
            val text = JsonObject()
            text.addProperty("path", "price/amount")
            add("text", text)
            addProperty("variant", "subtitle")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "fi-btn")
            addProperty("component", "Button")
            addProperty("text", "Add")
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

        // Offer Card
        comps.add(JsonObject().apply {
            addProperty("id", "offer-card")
            addProperty("component", "Card")
            addProperty("child", "offer-row")
            addProperty("margin", 16)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "offer-row")
            addProperty("component", "Row")
            add("children", JsonArray().apply { add("offer-icon"); add("offer-text-col") })
            addProperty("padding", 16)
            addProperty("align", "center")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "offer-icon")
            addProperty("component", "Icon")
            addProperty("name", "localOffer")
            addProperty("size", 40)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "offer-text-col")
            addProperty("component", "Column")
            add("children", JsonArray().apply { add("offer-title"); add("offer-desc") })
            addProperty("padding", 8)
        })

        comps.add(JsonObject().apply {
            addProperty("id", "offer-title")
            addProperty("component", "Text")
            addProperty("text", "50% OFF on First Order")
            addProperty("variant", "h4")
        })

        comps.add(JsonObject().apply {
            addProperty("id", "offer-desc")
            addProperty("component", "Text")
            addProperty("text", "Use code: LUXE50")
            addProperty("variant", "caption")
        })

        update.add("components", comps)
        root.add("updateComponents", update)
        return gson.toJson(root)
    }

    private fun addQuickActionComps(comps: JsonArray, id: String, icon: String, label: String, eventName: String) {
        comps.add(JsonObject().apply {
            addProperty("id", id)
            addProperty("component", "Card")
            addProperty("child", "$id-col")
            addProperty("weight", 1)
        })
        comps.add(JsonObject().apply {
            addProperty("id", "$id-col")
            addProperty("component", "Column")
            add("children", JsonArray().apply { add("$id-icon"); add("$id-text") })
            addProperty("align", "center")
            addProperty("padding", 8)
            val action = JsonObject()
            val event = JsonObject()
            event.addProperty("name", eventName)
            action.add("event", event)
            add("action", action)
        })
        comps.add(JsonObject().apply {
            addProperty("id", "$id-icon")
            addProperty("component", "Icon")
            addProperty("name", icon)
        })
        comps.add(JsonObject().apply {
            addProperty("id", "$id-text")
            addProperty("component", "Text")
            addProperty("text", label)
            addProperty("variant", "label")
        })
    }

    fun buildHomeData(featuredItems: List<MenuItem>): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        val data = JsonObject()
        data.add("featured", gson.toJsonTree(featuredItems))
        update.add("data", data)
        root.add("updateDataModel", update)
        return gson.toJson(root)
    }

    fun getSurfaceId() = surfaceId
}
