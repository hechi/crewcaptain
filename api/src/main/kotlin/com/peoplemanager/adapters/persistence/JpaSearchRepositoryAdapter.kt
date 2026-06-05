package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.port.output.SearchRepository
import com.peoplemanager.domain.SearchResult
import com.peoplemanager.domain.SearchResultType
import com.peoplemanager.domain.UserId
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Repository
class JpaSearchRepositoryAdapter(
    @PersistenceContext private val entityManager: EntityManager
) : SearchRepository {

    override fun search(
        userId: UserId,
        searchTerms: String,
        types: List<SearchResultType>?,
        offset: Int,
        limit: Int
    ): Pair<List<SearchResult>, Long> {
        val typeFilter = types?.map { it.name } ?: SearchResultType.entries.map { it.name }

        val unionQueries = buildList {
            if ("PERSON" in typeFilter) add(personSearchSql())
            if ("ONE_ON_ONE_ENTRY" in typeFilter) add(oneOnOneEntrySearchSql())
            if ("QUICK_NOTE" in typeFilter) add(quickNoteSearchSql())
            if ("ACTION_ITEM" in typeFilter) add(actionItemSearchSql())
            if ("PDP_GOAL" in typeFilter) add(pdpGoalSearchSql())
            if ("PDP_UPDATE" in typeFilter) add(pdpUpdateSearchSql())
            if ("KUDOS" in typeFilter) add(kudosSearchSql())
            if ("STRATEGY_GOAL" in typeFilter) add(strategyGoalSearchSql())
        }

        if (unionQueries.isEmpty()) {
            return Pair(emptyList(), 0L)
        }

        val unionSql = unionQueries.joinToString(" UNION ALL ")

        // Count query
        val countSql = "SELECT COUNT(*) FROM ($unionSql) AS search_results"
        val countQuery = entityManager.createNativeQuery(countSql)
        setQueryParameters(countQuery, userId, searchTerms, typeFilter)
        val totalCount = (countQuery.singleResult as Number).toLong()

        if (totalCount == 0L) {
            return Pair(emptyList(), 0L)
        }

        // Results query with pagination
        val resultsSql = """
            SELECT id, type, title, snippet, person_id, person_name, sensitive, created_at, relevance_score
            FROM ($unionSql) AS search_results
            ORDER BY relevance_score DESC, created_at DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val resultsQuery = entityManager.createNativeQuery(resultsSql)
        setQueryParameters(resultsQuery, userId, searchTerms, typeFilter)
        resultsQuery.setParameter("limit", limit)
        resultsQuery.setParameter("offset", offset)

        @Suppress("UNCHECKED_CAST")
        val rows = resultsQuery.resultList as List<Array<Any?>>

        val results = rows.map { row ->
            SearchResult(
                id = row[0] as UUID,
                type = SearchResultType.valueOf(row[1] as String),
                title = row[2] as String,
                snippet = row[3] as String?,
                personId = row[4] as UUID?,
                personName = row[5] as String?,
                sensitive = row[6] as Boolean,
                createdAt = when (val ts = row[7]) {
                    is Timestamp -> ts.toInstant()
                    is Instant -> ts
                    else -> Instant.now()
                },
                relevanceScore = (row[8] as Number).toDouble()
            )
        }

        return Pair(results, totalCount)
    }

    private fun setQueryParameters(
        query: jakarta.persistence.Query,
        userId: UserId,
        searchTerms: String,
        @Suppress("UNUSED_PARAMETER") typeFilter: List<String>
    ) {
        // Each sub-query uses :userId and :searchTerms
        query.setParameter("userId", userId.value)
        query.setParameter("searchTerms", searchTerms)
    }

    private fun personSearchSql(): String = """
        SELECT
            p.id AS id,
            'PERSON' AS type,
            p.name AS title,
            COALESCE(p.role_title, '') AS snippet,
            p.id AS person_id,
            p.name AS person_name,
            false AS sensitive,
            p.created_at AS created_at,
            ts_rank(
                persons_search_vector(p.name, p.preferred_name, p.role_title, p.email, p.tags),
                to_tsquery('english', CAST(:searchTerms AS text))
            ) AS relevance_score
        FROM persons p
        WHERE p.user_id = :userId
          AND persons_search_vector(p.name, p.preferred_name, p.role_title, p.email, p.tags)
              @@ to_tsquery('english', CAST(:searchTerms AS text))
    """.trimIndent()

    private fun oneOnOneEntrySearchSql(): String = """
        SELECT
            e.id AS id,
            'ONE_ON_ONE_ENTRY' AS type,
            COALESCE('1:1 on ' || TO_CHAR(e.meeting_date, 'YYYY-MM-DD'), '1:1 Entry') AS title,
            LEFT(COALESCE(e.notes_markdown, e.outcomes_markdown, ''), 200) AS snippet,
            e.person_id AS person_id,
            p.name AS person_name,
            e.sensitive AS sensitive,
            e.created_at AS created_at,
            ts_rank(
                one_on_one_entries_search_vector(e.notes_markdown, e.outcomes_markdown),
                to_tsquery('english', CAST(:searchTerms AS text))
            ) AS relevance_score
        FROM one_on_one_entries e
        JOIN persons p ON p.id = e.person_id AND p.user_id = e.user_id
        WHERE e.user_id = :userId
          AND e.sensitive = false
          AND one_on_one_entries_search_vector(e.notes_markdown, e.outcomes_markdown)
              @@ to_tsquery('english', CAST(:searchTerms AS text))
    """.trimIndent()

    private fun quickNoteSearchSql(): String = """
        SELECT
            qn.id AS id,
            'QUICK_NOTE' AS type,
            LEFT(qn.text, 100) AS title,
            LEFT(qn.text, 200) AS snippet,
            qn.person_id AS person_id,
            p.name AS person_name,
            qn.sensitive AS sensitive,
            qn.created_at AS created_at,
            ts_rank(
                quick_notes_search_vector(qn.text),
                to_tsquery('english', CAST(:searchTerms AS text))
            ) AS relevance_score
        FROM quick_notes qn
        LEFT JOIN persons p ON p.id = qn.person_id AND p.user_id = qn.user_id
        WHERE qn.user_id = :userId
          AND qn.sensitive = false
          AND quick_notes_search_vector(qn.text)
              @@ to_tsquery('english', CAST(:searchTerms AS text))
    """.trimIndent()

    private fun actionItemSearchSql(): String = """
        SELECT
            ai.id AS id,
            'ACTION_ITEM' AS type,
            ai.title AS title,
            LEFT(COALESCE(ai.description, ''), 200) AS snippet,
            ai.person_id AS person_id,
            p.name AS person_name,
            false AS sensitive,
            ai.created_at AS created_at,
            ts_rank(
                action_items_search_vector(ai.title, ai.description),
                to_tsquery('english', CAST(:searchTerms AS text))
            ) AS relevance_score
        FROM action_items ai
        JOIN persons p ON p.id = ai.person_id AND p.user_id = ai.user_id
        WHERE ai.user_id = :userId
          AND action_items_search_vector(ai.title, ai.description)
              @@ to_tsquery('english', CAST(:searchTerms AS text))
    """.trimIndent()

    private fun pdpGoalSearchSql(): String = """
        SELECT
            pg.id AS id,
            'PDP_GOAL' AS type,
            pg.title AS title,
            LEFT(COALESCE(pg.description, ''), 200) AS snippet,
            pg.person_id AS person_id,
            p.name AS person_name,
            false AS sensitive,
            pg.created_at AS created_at,
            ts_rank(
                pdp_goals_search_vector(pg.title, pg.description),
                to_tsquery('english', CAST(:searchTerms AS text))
            ) AS relevance_score
        FROM pdp_goals pg
        JOIN persons p ON p.id = pg.person_id AND p.user_id = pg.user_id
        WHERE pg.user_id = :userId
          AND pdp_goals_search_vector(pg.title, pg.description)
              @@ to_tsquery('english', CAST(:searchTerms AS text))
    """.trimIndent()

    private fun pdpUpdateSearchSql(): String = """
        SELECT
            pu.id AS id,
            'PDP_UPDATE' AS type,
            LEFT(pu.text_markdown, 100) AS title,
            LEFT(pu.text_markdown, 200) AS snippet,
            pg.person_id AS person_id,
            p.name AS person_name,
            pu.sensitive AS sensitive,
            pu.created_at AS created_at,
            ts_rank(
                pdp_updates_search_vector(pu.text_markdown),
                to_tsquery('english', CAST(:searchTerms AS text))
            ) AS relevance_score
        FROM pdp_updates pu
        JOIN pdp_goals pg ON pg.id = pu.goal_id
        JOIN persons p ON p.id = pg.person_id AND p.user_id = pg.user_id
        WHERE pg.user_id = :userId
          AND pu.sensitive = false
          AND pdp_updates_search_vector(pu.text_markdown)
              @@ to_tsquery('english', CAST(:searchTerms AS text))
    """.trimIndent()

    private fun kudosSearchSql(): String = """
        SELECT
            k.id AS id,
            'KUDOS' AS type,
            LEFT(k.text, 100) AS title,
            LEFT(k.text, 200) AS snippet,
            k.person_id AS person_id,
            p.name AS person_name,
            false AS sensitive,
            k.created_at AS created_at,
            ts_rank(
                kudos_search_vector(k.text, k.tags),
                to_tsquery('english', CAST(:searchTerms AS text))
            ) AS relevance_score
        FROM kudos k
        JOIN persons p ON p.id = k.person_id AND p.user_id = k.user_id
        WHERE k.user_id = :userId
          AND kudos_search_vector(k.text, k.tags)
              @@ to_tsquery('english', CAST(:searchTerms AS text))
    """.trimIndent()

    private fun strategyGoalSearchSql(): String = """
        SELECT
            sg.id AS id,
            'STRATEGY_GOAL' AS type,
            sg.title AS title,
            LEFT(COALESCE(sg.description, ''), 200) AS snippet,
            NULL AS person_id,
            NULL AS person_name,
            sg.sensitive AS sensitive,
            sg.created_at AS created_at,
            ts_rank(
                strategy_goals_search_vector(sg.title, sg.description),
                to_tsquery('english', CAST(:searchTerms AS text))
            ) AS relevance_score
        FROM strategy_goals sg
        WHERE sg.user_id = :userId
          AND sg.sensitive = false
          AND strategy_goals_search_vector(sg.title, sg.description)
              @@ to_tsquery('english', CAST(:searchTerms AS text))
    """.trimIndent()
}
