package com.peoplemanager.application.ports

import com.peoplemanager.application.queries.GenerateReviewPacketQuery

interface ReviewPacketPort {
    fun generateReviewPacket(query: GenerateReviewPacketQuery): String
}
