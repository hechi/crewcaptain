package com.peoplemanager.application.port.input

import com.peoplemanager.application.queries.CountOpenActionItemsQuery
import com.peoplemanager.application.queries.GetActionItemQuery
import com.peoplemanager.application.queries.ListActionItemsByPersonQuery
import com.peoplemanager.application.queries.ListAllActionItemsQuery
import com.peoplemanager.domain.ActionItem
import org.springframework.data.domain.Page

interface ActionItemQueryPort {
    fun getActionItem(query: GetActionItemQuery): ActionItem
    fun listActionItemsByPerson(query: ListActionItemsByPersonQuery): Page<ActionItem>
    fun listAllActionItems(query: ListAllActionItemsQuery): Page<ActionItem>
    fun countOpenActionItems(query: CountOpenActionItemsQuery): Long
}
