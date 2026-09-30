package com.peoplemanager.application.port.output

import com.peoplemanager.domain.FeedbackTemplate
import com.peoplemanager.domain.FeedbackTemplateId
import com.peoplemanager.domain.UserId

interface FeedbackTemplateRepository {
    fun save(template: FeedbackTemplate): FeedbackTemplate
    fun findByIdAndUserId(id: FeedbackTemplateId, userId: UserId): FeedbackTemplate?
    fun findAllByUserId(userId: UserId): List<FeedbackTemplate>
    fun deleteByIdAndUserId(id: FeedbackTemplateId, userId: UserId): Boolean
}
