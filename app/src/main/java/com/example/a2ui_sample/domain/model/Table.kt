package com.example.a2ui_sample.domain.model

import com.example.a2ui_sample.domain.valueobjects.TableId
import com.example.a2ui_sample.domain.valueobjects.TableStatus
import com.example.a2ui_sample.domain.valueobjects.Capacity

/**
 * Table Entity
 * Represents a physical table in the restaurant.
 */
data class Table(
    val id: TableId,
    val capacity: Capacity,
    val status: TableStatus = TableStatus.AVAILABLE,
    val locationDescription: String? = null
)
