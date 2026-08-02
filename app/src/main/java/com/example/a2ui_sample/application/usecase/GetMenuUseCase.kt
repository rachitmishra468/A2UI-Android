package com.example.a2ui_sample.application.usecase

import com.example.a2ui_sample.domain.model.MenuItem
import com.example.a2ui_sample.domain.repository.MenuRepository
import javax.inject.Inject

/**
 * GetMenuUseCase
 * Encapsulates the business logic for retrieving the full menu.
 */
class GetMenuUseCase @Inject constructor(
    private val menuRepository: MenuRepository
) {
    suspend operator fun invoke(): List<MenuItem> {
        return menuRepository.getMenuItems()
    }
}
