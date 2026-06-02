package com.peoplemanager.domain

import java.time.Instant

enum class StickyNoteColor {
    CYAN, PURPLE, GREEN, AMBER, PINK, SLATE;

    companion object {
        fun fromString(value: String?): StickyNoteColor =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: CYAN
    }
}

data class PinnedRememberItem(
    val id: RememberItemId,
    val text: String,
    val color: StickyNoteColor = StickyNoteColor.CYAN,
    val tag: String? = null,
    val sensitive: Boolean = false,
    val displayOrder: Int,
    val createdAt: Instant
) {
    init {
        require(text.isNotBlank()) { "Remember item text must not be blank" }
        require(tag == null || tag.length <= 30) { "Tag must be 30 characters or fewer" }
    }
}
