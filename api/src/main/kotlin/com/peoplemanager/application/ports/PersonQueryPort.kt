package com.peoplemanager.application.ports

import com.peoplemanager.application.queries.GetPersonQuery
import com.peoplemanager.application.queries.ListDeletedPersonsQuery
import com.peoplemanager.application.queries.ListPersonsQuery
import com.peoplemanager.domain.Person
import org.springframework.data.domain.Page

interface PersonQueryPort {
    fun getPerson(query: GetPersonQuery): Person
    fun listPersons(query: ListPersonsQuery): Page<Person>
    fun listDeletedPersons(query: ListDeletedPersonsQuery): Page<Person>
}
