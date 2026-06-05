package com.peoplemanager.application.port.output

import com.peoplemanager.application.queries.GenerateReviewPacketQuery

interface ReviewPacketPort {
    fun generateReviewPacket(query: GenerateReviewPacketQuery): String
}
