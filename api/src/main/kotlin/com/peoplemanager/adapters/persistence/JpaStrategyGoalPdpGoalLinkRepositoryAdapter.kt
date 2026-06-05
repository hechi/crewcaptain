package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.port.output.StrategyGoalPdpGoalLinkRepository
import com.peoplemanager.domain.PdpGoalId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.StrategyGoalId
import com.peoplemanager.domain.StrategyGoalPdpGoalLink
import com.peoplemanager.domain.StrategyGoalPdpGoalLinkId
import com.peoplemanager.domain.UserId
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class JpaStrategyGoalPdpGoalLinkRepositoryAdapter(
    private val springDataRepository: SpringDataStrategyGoalPdpGoalLinkRepository
) : StrategyGoalPdpGoalLinkRepository {

    override fun save(link: StrategyGoalPdpGoalLink): StrategyGoalPdpGoalLink {
        val entity = link.toEntity()
        return springDataRepository.save(entity).toDomain()
    }

    override fun findByIdAndUserId(linkId: StrategyGoalPdpGoalLinkId, userId: UserId): StrategyGoalPdpGoalLink? {
        return springDataRepository.findByIdAndUserId(linkId.value, userId.value)?.toDomain()
    }

    override fun findAllByStrategyGoalIdAndUserId(strategyGoalId: StrategyGoalId, userId: UserId): List<StrategyGoalPdpGoalLink> {
        return springDataRepository.findAllByStrategyGoalIdAndUserId(strategyGoalId.value, userId.value).map { it.toDomain() }
    }

    override fun findAllByPdpGoalIdAndUserId(pdpGoalId: PdpGoalId, userId: UserId): List<StrategyGoalPdpGoalLink> {
        return springDataRepository.findAllByPdpGoalIdAndUserId(pdpGoalId.value, userId.value).map { it.toDomain() }
    }

    override fun findAllByUserId(userId: UserId): List<StrategyGoalPdpGoalLink> {
        return springDataRepository.findAllByUserId(userId.value).map { it.toDomain() }
    }

    override fun findAllByUserIdAndPersonId(userId: UserId, personId: PersonId): List<StrategyGoalPdpGoalLink> {
        return springDataRepository.findAllByUserIdAndPersonId(userId.value, personId.value).map { it.toDomain() }
    }

    override fun existsByStrategyGoalIdAndPdpGoalIdAndUserId(
        strategyGoalId: StrategyGoalId,
        pdpGoalId: PdpGoalId,
        userId: UserId
    ): Boolean {
        return springDataRepository.existsByStrategyGoalIdAndPdpGoalIdAndUserId(
            strategyGoalId.value, pdpGoalId.value, userId.value
        )
    }

    override fun countByStrategyGoalIdAndUserId(strategyGoalId: StrategyGoalId, userId: UserId): Long {
        return springDataRepository.countByStrategyGoalIdAndUserId(strategyGoalId.value, userId.value)
    }

    override fun deleteByStrategyGoalIdAndPdpGoalIdAndUserId(
        strategyGoalId: StrategyGoalId,
        pdpGoalId: PdpGoalId,
        userId: UserId
    ): Boolean {
        val deleted = springDataRepository.deleteByStrategyGoalIdAndPdpGoalIdAndUserId(
            strategyGoalId.value, pdpGoalId.value, userId.value
        )
        return deleted > 0
    }

    override fun deleteByStrategyGoalIdAndUserId(strategyGoalId: StrategyGoalId, userId: UserId): Long {
        return springDataRepository.deleteByStrategyGoalIdAndUserId(strategyGoalId.value, userId.value)
    }

    override fun deleteByPdpGoalIdAndUserId(pdpGoalId: PdpGoalId, userId: UserId): Long {
        return springDataRepository.deleteByPdpGoalIdAndUserId(pdpGoalId.value, userId.value)
    }

    private fun StrategyGoalPdpGoalLinkEntity.toDomain(): StrategyGoalPdpGoalLink = StrategyGoalPdpGoalLink(
        id = StrategyGoalPdpGoalLinkId(this.id),
        userId = UserId(this.userId),
        strategyGoalId = StrategyGoalId(this.strategyGoalId),
        pdpGoalId = PdpGoalId(this.pdpGoalId),
        personId = PersonId(this.personId),
        createdAt = this.createdAt
    )

    private fun StrategyGoalPdpGoalLink.toEntity(): StrategyGoalPdpGoalLinkEntity = StrategyGoalPdpGoalLinkEntity(
        id = this.id.value,
        userId = this.userId.value,
        strategyGoalId = this.strategyGoalId.value,
        pdpGoalId = this.pdpGoalId.value,
        personId = this.personId.value,
        createdAt = this.createdAt
    )
}
