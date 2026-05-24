package com.peoplemanager.application

import com.peoplemanager.domain.ActionItemId

class ActionItemNotFoundException(val actionItemId: ActionItemId) :
    RuntimeException("Action item not found: ${actionItemId.value}")
