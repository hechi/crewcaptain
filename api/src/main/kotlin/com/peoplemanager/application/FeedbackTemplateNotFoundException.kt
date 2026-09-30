package com.peoplemanager.application

import com.peoplemanager.domain.FeedbackTemplateId

class FeedbackTemplateNotFoundException(templateId: FeedbackTemplateId) :
    RuntimeException("Feedback template not found: ${templateId.value}")
