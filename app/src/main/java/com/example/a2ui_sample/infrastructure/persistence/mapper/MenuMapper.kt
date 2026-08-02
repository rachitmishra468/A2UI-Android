package com.example.a2ui_sample.infrastructure.persistence.mapper

import com.example.a2ui_sample.domain.model.MenuItem
import com.example.a2ui_sample.domain.valueobjects.Price
import com.example.a2ui_sample.infrastructure.persistence.entity.MenuItemEntity

object MenuMapper {

    fun toDomain(entity: MenuItemEntity): MenuItem {
        return MenuItem(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            price = Price(entity.price),
            category = entity.category,
            type = entity.type,
            image = entity.imageUrl,
            rating = entity.rating,
            reviewCount = entity.reviewCount,
            isAvailable = entity.isAvailable
        )
    }

    fun toEntity(domain: MenuItem): MenuItemEntity {
        return MenuItemEntity(
            id = domain.id,
            name = domain.name,
            description = domain.description,
            price = domain.price.amount,
            category = domain.category,
            type = domain.type,
            imageUrl = domain.image,
            rating = domain.rating,
            reviewCount = domain.reviewCount,
            isAvailable = domain.isAvailable
        )
    }
}
