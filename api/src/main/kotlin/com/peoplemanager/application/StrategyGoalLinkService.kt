package com.peoplemanager.application

import com.peoplemanager.application.commands.*
import com.peoplemanager.application.ports.PdpGoalRepository
import com.peoplemanager.application.ports.StrategyGoalPdpGoalLinkRepository
import com.peoplemanager.application.ports.StrategyGoalRepository
import com.peoplemanager.domain.AuditLogEntry
import com.peoplemanager.domain.PdpGoalId
import com.peoplemanager.domain.PdpGoalStatus
import com.peoplemanager.domain.StrategyGoalId
import com.peoplemanager.domain.StrategyGoalPdpGoalLink
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class StrategyGoalLinkService(
    private val strategyGoalRepository: StrategyGoalRepository,
    private val pdpGoalRepository: PdpGoalRepository,
    private val linkRepository: StrategyGoalPdpGoalLinkRepository,
    private val auditLogService: AuditLogService
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
        // Get all active strategy goals for the user
        val pageable = PageRequest.of(0, 1000)
        val strategyGoals = strategyGoalRepository.findAllByUserId(
            userId, StrategyGoalStatus.ACTIVE, pageable
        ).content

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
        // This requires querying all persons and their PDP goals
        // For now, we'll return an empty list as this would require PersonRepository
        // In a full implementation, we'd inject PersonRepository and iterate through all persons
        return emptyList()
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
}
