package com.example.a2ui_sample.ui

import com.example.a2ui_sample.data.MenuItem
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * MenuA2UIBuilder
 * Generates the A2UI schema and data for the menu section.
 */
class MenuA2UIBuilder {
    private val gson = Gson()
    private val surfaceId = "home_menu_surface"

    /**
     * Creates the initial surface for the menu.
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
     * Builds the component schema for the menu section.
     */
    fun buildMenuSchema(): String {
        val root = JsonObject()
        root.addProperty("version", "v0.10")
        
        val update = JsonObject()
        update.addProperty("surfaceId", surfaceId)
        
        val components = JsonArray()

        // 1. Menu List (Column with template children)
        val menuList = JsonObject()
        menuList.addProperty("id", "root")
        menuList.addProperty("component", "Column")
        val childrenObj = JsonObject()
        childrenObj.addProperty("path", "/items")
        childrenObj.addProperty("componentId", "menu-card")
        menuList.add("children", childrenObj)
        components.add(menuList)

        // 2. Menu Card (Template)
        val menuCard = JsonObject()
        menuCard.addProperty("id", "menu-card")
        menuCard.addProperty("component", "Card")
        menuCard.addProperty("child", "menu-content")
        components.add(menuCard)

        // 3. Menu Content (Column)
        val menuContent = JsonObject()
        menuContent.addProperty("id", "menu-content")
        menuContent.addProperty("component", "Column")
        val contentChildren = JsonArray()
        contentChildren.add("food-image")
        contentChildren.add("food-name")
        contentChildren.add("food-category")
        contentChildren.add("food-description")
        contentChildren.add("food-price")
        contentChildren.add("add-button")
        menuContent.add("children", contentChildren)
        components.add(menuContent)

        // 4. Food Image
        val foodImage = JsonObject()
        foodImage.addProperty("id", "food-image")
        foodImage.addProperty("component", "Image")
        val imgUrl = JsonObject()
        imgUrl.addProperty("path", "image")
        foodImage.add("url", imgUrl)
        foodImage.addProperty("variant", "mediumFeature")
        components.add(foodImage)

        // 5. Food Name
        val foodName = JsonObject()
        foodName.addProperty("id", "food-name")
        foodName.addProperty("component", "Text")
        val namePath = JsonObject()
        namePath.addProperty("path", "name")
        foodName.add("text", namePath)
        foodName.addProperty("variant", "h3")
        components.add(foodName)

        // 6. Food Category
        val foodCategory = JsonObject()
        foodCategory.addProperty("id", "food-category")
        foodCategory.addProperty("component", "Text")
        val catPath = JsonObject()
        catPath.addProperty("path", "category")
        foodCategory.add("text", catPath)
        foodCategory.addProperty("variant", "caption")
        components.add(foodCategory)

        // 7. Food Description
        val foodDescription = JsonObject()
        foodDescription.addProperty("id", "food-description")
        foodDescription.addProperty("component", "Text")
        val descPath = JsonObject()
        descPath.addProperty("path", "description")
        foodDescription.add("text", descPath)
        foodDescription.addProperty("variant", "body")
        components.add(foodDescription)

        // 8. Food Price
        val foodPrice = JsonObject()
        foodPrice.addProperty("id", "food-price")
        foodPrice.addProperty("component", "Text")
        val pricePath = JsonObject()
        pricePath.addProperty("path", "price")
        foodPrice.add("text", pricePath)
        foodPrice.addProperty("variant", "h4")
        components.add(foodPrice)

        // 9. Add Button
        val addButton = JsonObject()
        addButton.addProperty("id", "add-button")
        addButton.addProperty("component", "Button")
        addButton.addProperty("label", "Add To Cart")
        
        val action = JsonObject()
        val event = JsonObject()
        event.addProperty("name", "add_to_cart")
        val context = JsonObject()
        val itemIdPath = JsonObject()
        itemIdPath.addProperty("path", "id")
        context.add("itemId", itemIdPath)
        event.add("context", context)
        action.add("event", event)
        addButton.add("action", action)
        
        components.add(addButton)

        update.add("components", components)
        root.add("updateComponents", update)
        
        return gson.toJson(root)
    }

    /**
     * Builds the data model update for the menu items.
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
