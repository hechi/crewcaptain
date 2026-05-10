package com.peoplemanager.domain

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.time.LocalDate

class CsvParserTest {

    private fun csvStream(content: String) = ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))

    @Nested
    inner class ParseCsvLine {

        @Test
        fun `should parse simple comma-separated values`() {
            val result = CsvParser.parseCsvLine("Alice,Engineer,alice@example.com")
            result shouldBe listOf("Alice", "Engineer", "alice@example.com")
        }

        @Test
        fun `should handle quoted fields with commas`() {
            val result = CsvParser.parseCsvLine("\"Smith, John\",Engineer,john@example.com")
            result shouldBe listOf("Smith, John", "Engineer", "john@example.com")
        }

        @Test
        fun `should handle escaped quotes inside quoted fields`() {
            val result = CsvParser.parseCsvLine("\"She said \"\"hello\"\"\",value2")
            result shouldBe listOf("She said \"hello\"", "value2")
        }

        @Test
        fun `should handle empty fields`() {
            val result = CsvParser.parseCsvLine("Alice,,alice@example.com")
            result shouldBe listOf("Alice", "", "alice@example.com")
        }

        @Test
        fun `should handle trailing comma`() {
            val result = CsvParser.parseCsvLine("Alice,Engineer,")
            result shouldBe listOf("Alice", "Engineer", "")
        }

        @Test
        fun `should handle single field`() {
            val result = CsvParser.parseCsvLine("Alice")
            result shouldBe listOf("Alice")
        }
    }

    @Nested
    inner class ParseCsv {

        @Test
        fun `should parse valid CSV with all fields`() {
            val csv = """
                name,preferred_name,role_title,timezone,start_date,email,tags
                Alice Smith,Ali,Senior Engineer,America/New_York,2023-01-15,alice@example.com,engineering|senior
            """.trimIndent()

            val result = CsvParser.parse(csvStream(csv))

            result.errors.shouldBeEmpty()
            result.rows shouldHaveSize 1
            result.rows[0].apply {
                name shouldBe "Alice Smith"
                preferredName shouldBe "Ali"
                roleTitle shouldBe "Senior Engineer"
                timezone shouldBe "America/New_York"
                startDate shouldBe LocalDate.of(2023, 1, 15)
                email shouldBe "alice@example.com"
                tags shouldBe listOf("engineering", "senior")
            }
        }

        @Test
        fun `should parse CSV with only required name field`() {
            val csv = """
                name
                Alice Smith
                Bob Jones
            """.trimIndent()

            val result = CsvParser.parse(csvStream(csv))

            result.errors.shouldBeEmpty()
            result.rows shouldHaveSize 2
            result.rows[0].name shouldBe "Alice Smith"
            result.rows[1].name shouldBe "Bob Jones"
        }

        @Test
        fun `should return error for empty CSV`() {
            val result = CsvParser.parse(csvStream(""))

            result.rows.shouldBeEmpty()
            result.errors shouldHaveSize 1
            result.errors[0] shouldContain "empty"
        }

        @Test
        fun `should return error for missing name header`() {
            val csv = """
                preferred_name,role_title,email
                Ali,Engineer,ali@example.com
            """.trimIndent()

            val result = CsvParser.parse(csvStream(csv))

            result.rows.shouldBeEmpty()
            result.errors shouldHaveSize 1
            result.errors[0] shouldContain "name"
        }

        @Test
        fun `should return error for row with blank name`() {
            val csv = """
                name,email
                ,alice@example.com
            """.trimIndent()

            val result = CsvParser.parse(csvStream(csv))

            result.rows.shouldBeEmpty()
            result.errors shouldHaveSize 1
            result.errors[0] shouldContain "Row 2"
            result.errors[0] shouldContain "blank"
        }

        @Test
        fun `should return error for invalid start_date format`() {
            val csv = """
                name,start_date
                Alice,not-a-date
            """.trimIndent()

            val result = CsvParser.parse(csvStream(csv))

            result.rows.shouldBeEmpty()
            result.errors shouldHaveSize 1
            result.errors[0] shouldContain "Row 2"
            result.errors[0] shouldContain "start_date"
        }

        @Test
        fun `should skip blank lines`() {
            val csv = "name,email\nAlice,alice@example.com\n\nBob,bob@example.com"

            val result = CsvParser.parse(csvStream(csv))

            result.errors.shouldBeEmpty()
            result.rows shouldHaveSize 2
        }

        @Test
        fun `should handle multiple rows with mixed valid and invalid`() {
            val csv = """
                name,start_date
                Alice,2023-01-15
                ,2023-02-01
                Charlie,invalid-date
                Dave,2023-04-01
            """.trimIndent()

            val result = CsvParser.parse(csvStream(csv))

            result.rows shouldHaveSize 2
            result.rows[0].name shouldBe "Alice"
            result.rows[1].name shouldBe "Dave"
            result.errors shouldHaveSize 2
        }

        @Test
        fun `should handle tags separated by pipe character`() {
            val csv = """
                name,tags
                Alice,engineering|senior|team-lead
            """.trimIndent()

            val result = CsvParser.parse(csvStream(csv))

            result.errors.shouldBeEmpty()
            result.rows[0].tags shouldBe listOf("engineering", "senior", "team-lead")
        }

        @Test
        fun `should handle empty tags field`() {
            val csv = """
                name,tags
                Alice,
            """.trimIndent()

            val result = CsvParser.parse(csvStream(csv))

            result.errors.shouldBeEmpty()
            result.rows[0].tags.shouldBeEmpty()
        }

        @Test
        fun `should ignore unknown headers gracefully`() {
            val csv = """
                name,unknown_field,email
                Alice,some_value,alice@example.com
            """.trimIndent()

            val result = CsvParser.parse(csvStream(csv))

            result.errors.shouldBeEmpty()
            result.rows shouldHaveSize 1
            result.rows[0].name shouldBe "Alice"
            result.rows[0].email shouldBe "alice@example.com"
        }

        @Test
        fun `should handle header-only CSV with no data rows`() {
            val csv = "name,email,role_title"

            val result = CsvParser.parse(csvStream(csv))

            result.rows.shouldBeEmpty()
            result.errors shouldHaveSize 1
            result.errors[0] shouldContain "no data rows"
        }

        @Test
        fun `should trim whitespace from values`() {
            val csv = """
                name,email,role_title
                 Alice Smith , alice@example.com , Senior Engineer 
            """.trimIndent()

            val result = CsvParser.parse(csvStream(csv))

            result.errors.shouldBeEmpty()
            result.rows[0].name shouldBe "Alice Smith"
            result.rows[0].email shouldBe "alice@example.com"
            result.rows[0].roleTitle shouldBe "Senior Engineer"
        }

        @Test
        fun `should handle case-insensitive headers`() {
            val csv = """
                Name,Preferred_Name,Role_Title
                Alice,Ali,Engineer
            """.trimIndent()

            val result = CsvParser.parse(csvStream(csv))

            result.errors.shouldBeEmpty()
            result.rows shouldHaveSize 1
            result.rows[0].name shouldBe "Alice"
            result.rows[0].preferredName shouldBe "Ali"
            result.rows[0].roleTitle shouldBe "Engineer"
        }

        @Test
        fun `should handle quoted fields in CSV`() {
            val csv = """
                name,role_title,email
                "Smith, Alice","Senior Engineer, Platform",alice@example.com
            """.trimIndent()

            val result = CsvParser.parse(csvStream(csv))

            result.errors.shouldBeEmpty()
            result.rows shouldHaveSize 1
            result.rows[0].name shouldBe "Smith, Alice"
            result.rows[0].roleTitle shouldBe "Senior Engineer, Platform"
        }
    }
}
