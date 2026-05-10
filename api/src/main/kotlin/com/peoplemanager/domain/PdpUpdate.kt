package com.peoplemanager.domain

import java.time.Instant

data class PdpUpdate(
    val id: PdpUpdateId,
    val goalId: PdpGoalId,
    val userId: UserId,
    val textMarkdown: String,
    val sensitive: Boolean = false,
    val createdAt: Instant = Instant.now()
) {
    init {
        require(textMarkdown.isNotBlank()) { "PDP update text must not be blank" }
    }
}
