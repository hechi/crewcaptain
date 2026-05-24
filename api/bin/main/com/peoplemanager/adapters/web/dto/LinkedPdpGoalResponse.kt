package com.peoplemanager.adapters.web.dto

import com.peoplemanager.application.StrategyGoalLinkService
import java.util.UUID

data class LinkedPdpGoalResponse(
    val pdpGoalId: UUID,
    val personId: UUID,
    val title: String
) {
    companion object {
        fun from(info: StrategyGoalLinkService.LinkedPdpGoalInfo): LinkedPdpGoalResponse =
            LinkedPdpGoalResponse(
                pdpGoalId = info.pdpGoalId.value,
                personId = info.personId.value,
                title = info.title
            )
    }
}
