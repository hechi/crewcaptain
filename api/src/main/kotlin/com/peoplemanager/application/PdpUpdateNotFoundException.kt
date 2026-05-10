package com.peoplemanager.application

import com.peoplemanager.domain.PdpUpdateId

class PdpUpdateNotFoundException(val updateId: PdpUpdateId) :
    RuntimeException("PDP update not found: ${updateId.value}")
