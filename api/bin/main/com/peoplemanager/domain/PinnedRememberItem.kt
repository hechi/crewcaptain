package com.peoplemanager.domain

import java.time.Instant

data class PinnedRememberItem(
    val id: RememberItemId,
    val text: String,
    val displayOrder: Int,
    val createdAt: Instant
) {
    init {
        require(text.isNotBlank()) { "Remember item text must not be blank" }
    }
}
