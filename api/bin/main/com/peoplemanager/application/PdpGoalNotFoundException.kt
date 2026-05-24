package com.peoplemanager.application

import com.peoplemanager.domain.PdpGoalId

class PdpGoalNotFoundException(val goalId: PdpGoalId) :
    RuntimeException("PDP goal not found: ${goalId.value}")
