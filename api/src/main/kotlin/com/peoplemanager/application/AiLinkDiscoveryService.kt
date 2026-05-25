package com.peoplemanager.application

import com.peoplemanager.application.ports.PdpGoalRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.ports.StrategyGoalRepository
import com.peoplemanager.domain.PdpGoalId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.StrategyGoalId
import com.peoplemanager.domain.StrategyGoalStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AiLinkDiscoveryService(
    private val strategyGoalRepository: StrategyGoalRepository,
    private val pdpGoalRepository: PdpGoalRepository,
    private val personRepository: PersonRepository,
    private val linkService: StrategyGoalLinkService
) {

    data class LinkSuggestion(
        val strategyGoalId: StrategyGoalId,
        val strategyGoalTitle: String,
        val pdpGoalId: PdpGoalId,
        val personId: PersonId,
        val pdpGoalTitle: String,
        val personName: String,
        val matchScore: Int,
        val reasoning: String
    )

    fun findLinkSuggestions(userId: com.peoplemanager.domain.UserId): List<LinkSuggestion> {
        val suggestions = mutableListOf<LinkSuggestion>()

        // Get all active strategy goals
        val pageable = PageRequest.of(0, 1000)
        val strategyGoals = strategyGoalRepository.findAllByUserId(
            userId, StrategyGoalStatus.ACTIVE, pageable
        ).content

        // Filter out sensitive strategy goals to preserve privacy — never suggest links to them
        val nonSensitiveStrategyGoals = strategyGoals.filter { !it.sensitive }

        // Get all active PDP goals across all persons
        val persons = personRepository.findAllByUserIdUnpaged(userId)
        val allPdpGoals = persons.flatMap { person ->
            pdpGoalRepository.findAllByUserIdAndPersonId(
                userId, person.id, com.peoplemanager.domain.PdpGoalStatus.ACTIVE, pageable
            ).content.map { goal ->
                Triple(goal, person.id, person.preferredName ?: person.name)
            }
        }

        // Get existing links to avoid suggesting already linked goals
        val existingLinks = linkService.getAllAlignmentScores(userId)
            .flatMap { score ->
                linkService.getLinkedPdpGoals(score.strategyGoalId, userId)
                    .map { it.pdpGoalId to score.strategyGoalId }
            }
            .toSet()

        // Simple keyword-based matching
        for (strategyGoal in nonSensitiveStrategyGoals) {
            val strategyKeywords = extractKeywords(strategyGoal.title + " " + (strategyGoal.description ?: ""))

            for ((pdpGoal, personId, personName) in allPdpGoals) {
                // Skip if already linked
                if (existingLinks.contains(pdpGoal.id to strategyGoal.id)) {
                    continue
                }

                val pdpKeywords = extractKeywords(pdpGoal.title + " " + (pdpGoal.description ?: ""))
                val matchScore = calculateMatchScore(strategyKeywords, pdpKeywords)

                if (matchScore >= 30) { // Minimum threshold for suggestion
                    suggestions.add(
                        LinkSuggestion(
                            strategyGoalId = strategyGoal.id,
                            strategyGoalTitle = strategyGoal.title,
                            pdpGoalId = pdpGoal.id,
                            personId = personId,
                            pdpGoalTitle = pdpGoal.title,
                            personName = personName,
                            matchScore = matchScore,
                            reasoning = generateReasoning(strategyGoal.title, pdpGoal.title, matchScore)
                        )
                    )
                }
            }
        }

        // Sort by match score descending
        return suggestions.sortedByDescending { it.matchScore }.take(10)
    }

    private fun extractKeywords(text: String): Set<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 }
            .filterNot { it in commonStopWords }
            .toSet()
    }

    private fun calculateMatchScore(strategyKeywords: Set<String>, pdpKeywords: Set<String>): Int {
        if (strategyKeywords.isEmpty() || pdpKeywords.isEmpty()) {
            return 0
        }

        val intersection = strategyKeywords.intersect(pdpKeywords)
        val union = strategyKeywords.union(pdpKeywords)

        // Jaccard similarity as percentage
        return ((intersection.size.toDouble() / union.size) * 100).toInt()
    }

    private fun generateReasoning(strategyTitle: String, pdpTitle: String, score: Int): String {
        return when {
            score >= 70 -> "Strong match: Both goals share significant thematic overlap"
            score >= 50 -> "Good match: Related concepts and objectives"
            score >= 30 -> "Potential match: Some shared keywords or themes"
            else -> "Weak match"
        }
    }

    companion object {
        private val commonStopWords = setOf(
            "the", "and", "for", "with", "you", "this", "that", "have", "from",
            "they", "she", "been", "their", "will", "would", "there", "could",
            "should", "about", "into", "through", "during", "before", "after",
            "above", "below", "between", "under", "again", "further", "then",
            "once", "here", "when", "where", "what", "how", "all", "each",
            "which", "who", "why", "can", "may", "must", "shall"
        )
    }
}
