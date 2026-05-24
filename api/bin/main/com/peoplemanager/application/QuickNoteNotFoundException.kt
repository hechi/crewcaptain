package com.peoplemanager.application

import com.peoplemanager.domain.QuickNoteId

class QuickNoteNotFoundException(quickNoteId: QuickNoteId) :
    RuntimeException("Quick note not found: ${quickNoteId.value}")
