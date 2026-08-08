package com.example.a2ui_sample.presentation.a2ui

import com.example.a2ui_sample.domain.model.MenuItem
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * MenuA2UIBuilder
 * Modular A2UI Builder that separates components for reusability.
 * Fixes data visibility by ensuring correct paths for MenuItem properties.
 */
class MenuA2UIBuilder {
    private val gson = Gson()
    private val surfaceId = "home_menu_surface"

    /**
     * Creates the initial surface.
     */
    fun createSurfaceJson(): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        val createSurface = JsonObject()
        createSurface.addProperty("surfaceId", surfaceId)
        createSurface.addProperty("catalogId", "restaurant")
        root.add("createSurface", createSurface)
        return gson.toJson(root)
    }

    /**
     * MODULAR COMPONENTS
     */

    private fun createImageComponent(): JsonObject {
        val comp = JsonObject()
        comp.addProperty("id", "food-image")
        comp.addProperty("component", "Image")
        val url = JsonObject()
        url.addProperty("path", "image") // Maps to MenuItem.image
        comp.add("url", url)
        comp.addProperty("variant", "avatar")
        comp.addProperty("variant", "mediumFeature")
        return comp
    }

    private fun createNameComponent(): JsonObject {
        val comp = JsonObject()
        comp.addProperty("id", "food-name")
        comp.addProperty("component", "Text")
        val text = JsonObject()
        text.addProperty("path", "name") // Maps to MenuItem.name
        comp.add("text", text)
        comp.addProperty("variant", "h3")
        return comp
    }

    private fun createCategoryComponent(): JsonObject {
        val comp = JsonObject()
        comp.addProperty("id", "food-category")
        comp.addProperty("component", "Text")
        val text = JsonObject()
        text.addProperty("path", "category") // Maps to MenuItem.category
        comp.add("text", text)
        comp.addProperty("variant", "caption")
        return comp
    }

    private fun createRatingComponent(): JsonObject {
        val comp = JsonObject()
        comp.addProperty("id", "food-rating")
        comp.addProperty("component", "Text")
        val text = JsonObject()
        text.addProperty("path", "rating") // Maps to MenuItem.rating
        comp.add("text", text)
        comp.addProperty("variant", "body")
        return comp
    }

    private fun createDescriptionComponent(): JsonObject {
        val comp = JsonObject()
        comp.addProperty("id", "food-description")
        comp.addProperty("component", "Text")
        val text = JsonObject()
        text.addProperty("path", "description") // Maps to MenuItem.description
        comp.add("text", text)
        comp.addProperty("variant", "body")
        return comp
    }

    private fun createPriceComponent(): JsonObject {
        val comp = JsonObject()
        comp.addProperty("id", "food-price")
        comp.addProperty("component", "Text")
        val text = JsonObject()
        text.addProperty("template", "₹{price/amount}") // Maps to Price.amount with Rupee symbol
        comp.add("text", text)
        comp.addProperty("variant", "h4")
        return comp
    }

    private fun createAddButtonComponent(): JsonObject {
        val comp = JsonObject()
        comp.addProperty("id", "add-button")
        comp.addProperty("component", "Button")
        comp.addProperty("text", "Add To Cart")

        val action = JsonObject()
        val event = JsonObject()
        event.addProperty("name", "add_to_cart")
        val context = JsonObject()
        val itemId = JsonObject()
        itemId.addProperty("path", "id") // Maps to MenuItem.id
        context.add("itemId", itemId)
        event.add("context", context)
        action.add("event", event)
        comp.add("action", action)
        return comp
    }

    private fun createMenuCardComponent(): JsonObject {
        val comp = JsonObject()
        comp.addProperty("id", "menu-card")
        comp.addProperty("component", "Card")
        comp.addProperty("child", "menu-content")
        return comp
    }

    private fun createMenuContentComponent(): JsonObject {
        val comp = JsonObject()
        comp.addProperty("id", "menu-content")
        comp.addProperty("component", "Column")
        val children = JsonArray()
        children.add("food-image")
        children.add("food-name")
        children.add("food-category")
        children.add("food-rating")
        children.add("food-description")
        children.add("food-price")
        children.add("add-button")
        comp.add("children", children)
        return comp
    }

    private fun createRootListComponent(): JsonObject {
        val comp = JsonObject()
        comp.addProperty("id", "root")
        comp.addProperty("component", "List")
        comp.addProperty("direction", "horizontal")
        val children = JsonObject()
        children.addProperty("path", "/items")
        children.addProperty("componentId", "menu-card")
        comp.add("children", children)
        return comp
    }

    /**
     * Builds the full schema by combining modular components.
     */
    fun buildMenuSchema(): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")

        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)

        val components = JsonArray()
        components.add(createRootListComponent())
        components.add(createMenuCardComponent())
        components.add(createMenuContentComponent())
        components.add(createImageComponent())
        components.add(createNameComponent())
        components.add(createCategoryComponent())
        components.add(createRatingComponent())
        components.add(createDescriptionComponent())
        components.add(createPriceComponent())
        components.add(createAddButtonComponent())

        update.add("components", components)
        root.add("updateComponents", update)

        return gson.toJson(root)
    }

    /**
     * Updates the data model with the list of menu items.
     */
    fun buildMenuData(items: List<MenuItem>): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")

        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        update.addProperty("path", "/items")
        update.add("value", gson.toJsonTree(items))

        root.add("updateDataModel", update)

        return gson.toJson(root)
    }

    fun getSurfaceId() = surfaceId
}
