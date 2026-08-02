package com.example.a2ui_sample.application.agent

import com.example.a2ui_sample.application.usecase.GetMenuUseCase
import com.example.a2ui_sample.application.usecase.SearchMenuUseCase
import com.example.a2ui_sample.domain.model.MenuItem
import com.example.a2ui_sample.domain.valueobjects.MenuItemType
import com.example.a2ui_sample.domain.valueobjects.Price
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class MenuAgentTest {

    private lateinit var searchMenuUseCase: SearchMenuUseCase
    private lateinit var getMenuUseCase: GetMenuUseCase
    private lateinit var a2uiBuilder: A2UIResponseBuilder
    private lateinit var menuAgent: MenuAgent

    @Before
    fun setUp() {
        searchMenuUseCase = mock(SearchMenuUseCase::class.java)
        getMenuUseCase = mock(GetMenuUseCase::class.java)
        a2uiBuilder = A2UIResponseBuilder()
        menuAgent = MenuAgent(searchMenuUseCase, getMenuUseCase, a2uiBuilder)
    }

    @Test
    fun `process search query returns menu display with a2ui json`() = runBlocking {
        val query = "Pizza"
        val mockItems = listOf(
            MenuItem(1, "Pizza", "Cheesy", Price(500), "Italian", MenuItemType.NONVEG, "")
        )
        `when`(searchMenuUseCase.invoke(query)).thenReturn(mockItems)

        val context = AgentContext("CUST-001", "TRACE-123")
        val response = menuAgent.process(query, context)

        assertTrue(response is AgentResponse.MenuDisplay)
        val menuResponse = response as AgentResponse.MenuDisplay
        assertNotNull(menuResponse.a2uiJson)
        assertTrue(menuResponse.a2uiJson!!.contains("menu_grid"))
    }
}
