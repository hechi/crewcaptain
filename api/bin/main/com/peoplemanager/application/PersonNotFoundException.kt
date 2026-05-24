package com.peoplemanager.application

import com.peoplemanager.domain.PersonId

class PersonNotFoundException(val personId: PersonId) : RuntimeException("Person not found: ${personId.value}")
