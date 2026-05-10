package com.peoplemanager.application.queries

import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GenerateReviewPacketQueryTest {

    private val userId = UserId.generate()
    private val personId = PersonId.generate()

    @Test
    fun `should create query with valid date range`() {
        val query = GenerateReviewPacketQuery(
            userId = userId,
            personId = personId,
            dateFrom = LocalDate.of(2024, 1, 1),
            dateTo = LocalDate.of(2024, 6, 30)
        )

        query.dateFrom shouldBe LocalDate.of(2024, 1, 1)
        query.dateTo shouldBe LocalDate.of(2024, 6, 30)
    }

    @Test
    fun `should allow same date for dateFrom and dateTo`() {
        val date = LocalDate.of(2024, 3, 15)
        val query = GenerateReviewPacketQuery(
            userId = userId,
            personId = personId,
            dateFrom = date,
            dateTo = date
        )

        query.dateFrom shouldBe date
        query.dateTo shouldBe date
    }

    @Test
    fun `should throw when dateTo is before dateFrom`() {
        shouldThrow<IllegalArgumentException> {
            GenerateReviewPacketQuery(
                userId = userId,
                personId = personId,
                dateFrom = LocalDate.of(2024, 6, 30),
                dateTo = LocalDate.of(2024, 1, 1)
            )
        }
    }
}
