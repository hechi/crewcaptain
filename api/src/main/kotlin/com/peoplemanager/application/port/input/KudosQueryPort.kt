package com.peoplemanager.application.port.input

import com.peoplemanager.application.queries.GetKudosQuery
import com.peoplemanager.application.queries.ListAllKudosQuery
import com.peoplemanager.application.queries.ListKudosByPersonQuery
import com.peoplemanager.domain.Kudos
import org.springframework.data.domain.Page

interface KudosQueryPort {
    fun getKudos(query: GetKudosQuery): Kudos
    fun listKudosByPerson(query: ListKudosByPersonQuery): Page<Kudos>
    fun listAllKudos(query: ListAllKudosQuery): Page<Kudos>
}
