package com.peoplemanager.application.port.input

import com.peoplemanager.application.commands.*
import com.peoplemanager.domain.PdpGoal
import com.peoplemanager.domain.PdpUpdate

interface PdpGoalCommandPort {
    fun createPdpGoal(command: CreatePdpGoalCommand): PdpGoal
    fun updatePdpGoal(command: UpdatePdpGoalCommand): PdpGoal
    fun achievePdpGoal(command: AchievePdpGoalCommand): PdpGoal
    fun pausePdpGoal(command: PausePdpGoalCommand): PdpGoal
    fun dropPdpGoal(command: DropPdpGoalCommand): PdpGoal
    fun resumePdpGoal(command: ResumePdpGoalCommand): PdpGoal
    fun deletePdpGoal(command: DeletePdpGoalCommand)
    fun addPdpUpdate(command: AddPdpUpdateCommand): PdpUpdate
    fun deletePdpUpdate(command: DeletePdpUpdateCommand)
}
