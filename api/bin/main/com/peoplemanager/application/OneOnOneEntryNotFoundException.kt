package com.peoplemanager.application

import com.peoplemanager.domain.OneOnOneEntryId

class OneOnOneEntryNotFoundException(val entryId: OneOnOneEntryId) :
    RuntimeException("1:1 entry not found: ${entryId.value}")
