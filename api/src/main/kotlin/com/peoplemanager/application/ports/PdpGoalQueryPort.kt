package com.peoplemanager.application.ports

import com.peoplemanager.application.queries.CountActivePdpGoalsQuery
import com.peoplemanager.application.queries.GetPdpGoalQuery
import com.peoplemanager.application.queries.ListPdpGoalsByPersonQuery
import com.peoplemanager.application.queries.ListPdpUpdatesByGoalQuery
import com.peoplemanager.domain.PdpGoal
import com.peoplemanager.domain.PdpUpdate
import org.springframework.data.domain.Page

interface PdpGoalQueryPort {
    fun getPdpGoal(query: GetPdpGoalQuery): PdpGoal
    fun listPdpGoalsByPerson(query: ListPdpGoalsByPersonQuery): Page<PdpGoal>
    fun listPdpUpdatesByGoal(query: ListPdpUpdatesByGoalQuery): Page<PdpUpdate>
    fun countActivePdpGoals(query: CountActivePdpGoalsQuery): Long
}
