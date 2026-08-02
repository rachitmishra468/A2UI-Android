package com.example.a2ui_sample.application.usecase

import com.example.a2ui_sample.domain.model.MenuItem
import com.example.a2ui_sample.domain.repository.MenuRepository
import javax.inject.Inject

/**
 * SearchMenuUseCase
 * Encapsulates the business logic for searching the menu.
 */
class SearchMenuUseCase @Inject constructor(
    private val menuRepository: MenuRepository
) {
    suspend operator fun invoke(query: String): List<MenuItem> {
        if (query.isBlank()) return emptyList()
        return menuRepository.searchMenu(query)
    }
}
