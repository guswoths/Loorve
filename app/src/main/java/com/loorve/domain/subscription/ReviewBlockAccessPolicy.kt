package com.loorve.domain.subscription

import com.loorve.domain.model.ReviewBlock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ReviewBlockAccessPolicy {
    private val seoulZone = ZoneId.of("Asia/Seoul")

    fun sortBlocks(
        blocks: List<ReviewBlock>,
        today: LocalDate = LocalDate.now(seoulZone)
    ): List<ReviewBlock> {
        return blocks.sortedWith(
            compareBy<ReviewBlock> { block ->
                if (endDate(block).isBefore(today)) 1 else 0
            }
                .thenBy(::endDate)
                .thenBy { it.createdAt }
                .thenBy { it.blockId }
        )
    }

    fun accessibleBlockIds(
        blocks: List<ReviewBlock>,
        entitlement: SubscriptionEntitlement
    ): Set<String> {
        if (entitlement is SubscriptionEntitlement.Pro) {
            return blocks.mapNotNull { it.blockId.takeIf(String::isNotBlank) }.toSet()
        }

        return sortBlocks(blocks)
            .firstOrNull()
            ?.blockId
            ?.takeIf(String::isNotBlank)
            ?.let(::setOf)
            .orEmpty()
    }

    fun canCreate(blocks: List<ReviewBlock>, entitlement: SubscriptionEntitlement): Boolean {
        return entitlement is SubscriptionEntitlement.Pro || blocks.isEmpty()
    }

    private fun endDate(block: ReviewBlock): LocalDate {
        if (block.examDate > 0L) {
            return Instant.ofEpochMilli(block.examDate)
                .atZone(seoulZone)
                .toLocalDate()
        }
        return runCatching { LocalDate.parse(block.date) }.getOrDefault(LocalDate.MAX)
    }
}
