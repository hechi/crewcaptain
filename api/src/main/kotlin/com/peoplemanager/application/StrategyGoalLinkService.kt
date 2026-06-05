package com.peoplemanager.application

import com.peoplemanager.application.commands.*
import com.peoplemanager.application.port.output.PdpGoalRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.port.output.StrategyGoalPdpGoalLinkRepository
import com.peoplemanager.application.port.output.StrategyGoalRepository
import com.peoplemanager.domain.AuditLogEntry
import com.peoplemanager.domain.PdpGoalId
import com.peoplemanager.domain.StrategyGoalId
import com.peoplemanager.domain.StrategyGoalPdpGoalLink
import com.peoplemanager.domain.StrategyGoalStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class StrategyGoalLinkService(
    private val strategyGoalRepository: StrategyGoalRepository,
    private val pdpGoalRepository: PdpGoalRepository,
    private val linkRepository: StrategyGoalPdpGoalLinkRepository,
    private val auditLogService: AuditLogService,
    private val personRepository: PersonRepository
) {

    data class AlignmentScore(
        val strategyGoalId: StrategyGoalId,
        val strategyGoalTitle: String,
        val totalActivePdpGoals: Int,
        val linkedPdpGoals: Int,
        val alignmentPercentage: Int
    )

    data class GapAnalysis(
        val unlinkedPdpGoals: List<UnlinkedPdpGoalInfo>,
        val emptyStrategyGoals: List<EmptyStrategyGoalInfo>
    )

    data class UnlinkedPdpGoalInfo(
        val pdpGoalId: PdpGoalId,
        val personId: com.peoplemanager.domain.PersonId,
        val title: String
    )

    data class EmptyStrategyGoalInfo(
        val strategyGoalId: StrategyGoalId,
        val title: String
    )

    fun linkPdpGoal(command: LinkPdpGoalToStrategyGoalCommand) {
        // Verify strategy goal belongs to user
        val strategyGoal = strategyGoalRepository.findByIdAndUserId(command.strategyGoalId, command.userId)
            ?: throw StrategyGoalNotFoundException(command.strategyGoalId)

        // Verify PDP goal belongs to user
        val pdpGoal = pdpGoalRepository.findByIdAndUserIdAndPersonId(
            command.pdpGoalId, command.userId, command.personId
        ) ?: throw PdpGoalNotFoundException(command.pdpGoalId)

        // Check if link already exists
        if (linkRepository.existsByStrategyGoalIdAndPdpGoalIdAndUserId(
                command.strategyGoalId, command.pdpGoalId, command.userId
            )) {
            return // Already linked, idempotent
        }

        // Create the link
        val link = StrategyGoalPdpGoalLink.create(
            userId = command.userId,
            strategyGoalId = command.strategyGoalId,
            pdpGoalId = command.pdpGoalId,
            personId = command.personId
        )

        linkRepository.save(link)
        auditLogService.record(
            AuditLogEntry.strategyGoalLinked(
                command.userId,
                command.strategyGoalId,
                strategyGoal.title,
                pdpGoal.title
            )
        )
    }

    fun unlinkPdpGoal(command: UnlinkPdpGoalFromStrategyGoalCommand) {
        // Verify strategy goal belongs to user
        val strategyGoal = strategyGoalRepository.findByIdAndUserId(command.strategyGoalId, command.userId)
            ?: throw StrategyGoalNotFoundException(command.strategyGoalId)

        // Find the link to get PDP goal info for audit log
        val links = linkRepository.findAllByStrategyGoalIdAndUserId(command.strategyGoalId, command.userId)
        val link = links.find { it.pdpGoalId == command.pdpGoalId }

        if (link != null) {
            // Get PDP goal title for audit log
            val pdpGoal = pdpGoalRepository.findByIdAndUserIdAndPersonId(
                command.pdpGoalId, command.userId, link.personId
            )

            linkRepository.deleteByStrategyGoalIdAndPdpGoalIdAndUserId(
                command.strategyGoalId, command.pdpGoalId, command.userId
            )

            auditLogService.record(
                AuditLogEntry.strategyGoalUnlinked(
                    command.userId,
                    command.strategyGoalId,
                    strategyGoal.title,
                    pdpGoal?.title ?: "Unknown"
                )
            )
        }
    }

    fun getAlignmentScore(strategyGoalId: StrategyGoalId, userId: com.peoplemanager.domain.UserId): AlignmentScore {
        val strategyGoal = strategyGoalRepository.findByIdAndUserId(strategyGoalId, userId)
            ?: throw StrategyGoalNotFoundException(strategyGoalId)

        // Get all links for this strategy goal
        val links = linkRepository.findAllByStrategyGoalIdAndUserId(strategyGoalId, userId)
        val linkedPdpGoals = links.size

        // Count total ACTIVE PDP goals for the user (across all persons)
        // We need to iterate through all persons to get total count
        val totalActivePdpGoals = getTotalActivePdpGoalsForUser(userId)

        val alignmentPercentage = if (totalActivePdpGoals > 0) {
            (linkedPdpGoals.toDouble() / totalActivePdpGoals * 100).toInt()
        } else {
            0
        }

        return AlignmentScore(
            strategyGoalId = strategyGoalId,
            strategyGoalTitle = strategyGoal.title,
            totalActivePdpGoals = totalActivePdpGoals,
            linkedPdpGoals = linkedPdpGoals,
            alignmentPercentage = alignmentPercentage
        )
    }

    fun getAllAlignmentScores(userId: com.peoplemanager.domain.UserId): List<AlignmentScore> {
        // Get ALL strategy goals for the user (all statuses, not just ACTIVE)
        val pageable = PageRequest.of(0, 1000)
        val strategyGoals = strategyGoalRepository.findAllByUserId(userId, null, pageable).content

        return strategyGoals.map { getAlignmentScore(it.id, userId) }
    }

    fun getGapAnalysis(userId: com.peoplemanager.domain.UserId): GapAnalysis {
        // Find all PDP goals that are not linked to any strategy goal
        val allLinks = linkRepository.findAllByUserId(userId)
        val linkedPdpGoalIds = allLinks.map { it.pdpGoalId }.toSet()

        // Get all active PDP goals for the user
        // This requires iterating through all persons
        val allPdpGoals = getAllActivePdpGoalsForUser(userId)
        val unlinkedPdpGoals = allPdpGoals
            .filter { it.id !in linkedPdpGoalIds }
            .map {
                UnlinkedPdpGoalInfo(
                    pdpGoalId = it.id,
                    personId = it.personId,
                    title = it.title
                )
            }

        // Find all strategy goals with no linked PDP goals
        val pageable = PageRequest.of(0, 1000)
        val allStrategyGoals = strategyGoalRepository.findAllByUserId(userId, null, pageable).content
        val emptyStrategyGoals = allStrategyGoals
            .filter { strategyGoal ->
                val linkCount = linkRepository.countByStrategyGoalIdAndUserId(strategyGoal.id, userId)
                linkCount == 0L
            }
            .map {
                EmptyStrategyGoalInfo(
                    strategyGoalId = it.id,
                    title = it.title
                )
            }

        return GapAnalysis(
            unlinkedPdpGoals = unlinkedPdpGoals,
            emptyStrategyGoals = emptyStrategyGoals
        )
    }

    private fun getTotalActivePdpGoalsForUser(userId: com.peoplemanager.domain.UserId): Int {
        // This is a simplified implementation
        // In a real implementation, we might want to cache this or use a more efficient query
        val allPdpGoals = getAllActivePdpGoalsForUser(userId)
        return allPdpGoals.size
    }

    private fun getAllActivePdpGoalsForUser(userId: com.peoplemanager.domain.UserId): List<com.peoplemanager.domain.PdpGoal> {
        // Gather all ACTIVE PDP goals across all persons for the user
        val persons = personRepository.findAllByUserIdUnpaged(userId)
        val pageable = PageRequest.of(0, 1000)
        val results = mutableListOf<com.peoplemanager.domain.PdpGoal>()

        for (person in persons) {
            val page = pdpGoalRepository.findAllByUserIdAndPersonId(userId, person.id, com.peoplemanager.domain.PdpGoalStatus.ACTIVE, pageable)
            results.addAll(page.content)
        }

        return results
    }

    data class LinkedPdpGoalInfo(
        val pdpGoalId: PdpGoalId,
        val personId: com.peoplemanager.domain.PersonId,
        val title: String
    )

    fun getLinkedPdpGoals(strategyGoalId: StrategyGoalId, userId: com.peoplemanager.domain.UserId): List<LinkedPdpGoalInfo> {
        strategyGoalRepository.findByIdAndUserId(strategyGoalId, userId)
            ?: throw StrategyGoalNotFoundException(strategyGoalId)

        val links = linkRepository.findAllByStrategyGoalIdAndUserId(strategyGoalId, userId)

        return links.map { link ->
            val pdpGoal = pdpGoalRepository.findByIdAndUserIdAndPersonId(
                link.pdpGoalId, userId, link.personId
            )
            LinkedPdpGoalInfo(
                pdpGoalId = link.pdpGoalId,
                personId = link.personId,
                title = pdpGoal?.title ?: "Unknown"
            )
        }
    }

    data class StrategyGoalBasicInfo(
        val strategyGoalId: StrategyGoalId,
        val title: String,
        val status: StrategyGoalStatus
    )

    fun getStrategyGoalsByPdpGoal(
        pdpGoalId: PdpGoalId,
        personId: com.peoplemanager.domain.PersonId,
        userId: com.peoplemanager.domain.UserId
    ): List<StrategyGoalBasicInfo> {
        // Validate that the PDP goal belongs to the specified person
        val pdpGoal = pdpGoalRepository.findByIdAndUserIdAndPersonId(pdpGoalId, userId, personId)
            ?: throw PdpGoalNotFoundException(pdpGoalId)

        // Find all links for this PDP goal
        val allLinks = linkRepository.findAllByUserId(userId)
        val linksForPdpGoal = allLinks.filter { it.pdpGoalId == pdpGoalId }

        // Fetch the strategy goal details for each link
        return linksForPdpGoal.mapNotNull { link ->
            val strategyGoal = strategyGoalRepository.findByIdAndUserId(link.strategyGoalId, userId)
            strategyGoal?.let {
                StrategyGoalBasicInfo(
                    strategyGoalId = it.id,
                    title = it.title,
                    status = it.status
                )
            }
        }
    }
}
