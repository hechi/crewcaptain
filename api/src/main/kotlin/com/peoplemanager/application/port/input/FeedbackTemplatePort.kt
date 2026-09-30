package com.peoplemanager.application.port.input

import com.peoplemanager.application.commands.CreateFeedbackTemplateCommand
import com.peoplemanager.application.commands.DeleteFeedbackTemplateCommand
import com.peoplemanager.application.commands.UpdateFeedbackTemplateCommand
import com.peoplemanager.application.queries.GetFeedbackTemplateQuery
import com.peoplemanager.application.queries.ListFeedbackTemplatesQuery
import com.peoplemanager.domain.FeedbackTemplate

interface FeedbackTemplateCommandPort {
    fun createTemplate(command: CreateFeedbackTemplateCommand): FeedbackTemplate
    fun updateTemplate(command: UpdateFeedbackTemplateCommand): FeedbackTemplate
    fun deleteTemplate(command: DeleteFeedbackTemplateCommand)
}

interface FeedbackTemplateQueryPort {
    fun getTemplate(query: GetFeedbackTemplateQuery): FeedbackTemplate
    fun listTemplates(query: ListFeedbackTemplatesQuery): List<FeedbackTemplate>

    /** Built-in starter templates offered to all managers (not persisted until saved). */
    fun listStarterTemplates(): List<FeedbackTemplate>
}
