package com.example.a2ui_sample.domain.usecase

import com.example.a2ui_sample.application.usecase.SearchMenuUseCase
import com.example.a2ui_sample.domain.model.MenuItem
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.example.a2ui_sample.domain.valueobjects.MenuItemType
import com.example.a2ui_sample.domain.valueobjects.Price
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import kotlinx.coroutines.test.runTest

class SearchMenuUseCaseTest {

    private lateinit var menuRepository: MenuRepository
    private lateinit var searchMenuUseCase: SearchMenuUseCase

    @Before
    fun setUp() {
        menuRepository = mock(MenuRepository::class.java)
        searchMenuUseCase = SearchMenuUseCase(menuRepository)
    }

    @Test
    fun `when query is blank, return empty list`() = runTest {
        val result = searchMenuUseCase("")
        assertEquals(0, result.size)
    }

    @Test
    fun `when query is valid, return results from repository`() = runTest {
        val mockItems = listOf(
            MenuItem(1, "Burger", "Tasty", Price(100), "Fast Food", MenuItemType.NONVEG, "")
        )
        `when`(menuRepository.searchMenu("Burger")).thenReturn(mockItems)

        val result = searchMenuUseCase("Burger")
        
        assertEquals(1, result.size)
        assertEquals("Burger", result[0].name)
    }
}
