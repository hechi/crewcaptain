package com.peoplemanager.application

import com.peoplemanager.domain.KudosId

class KudosNotFoundException(val kudosId: KudosId) :
    RuntimeException("Kudos not found: ${kudosId.value}")
