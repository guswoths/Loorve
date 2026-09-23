package com.loorve.domain.subscription

import com.loorve.domain.model.ReviewBlock

object ReviewBlockAccessPolicy {
    fun accessibleBlockIds(
        blocks: List<ReviewBlock>,
        entitlement: SubscriptionEntitlement
    ): Set<String> {
        if (entitlement is SubscriptionEntitlement.Pro) {
            return blocks.mapNotNull { it.blockId.takeIf(String::isNotBlank) }.toSet()
        }

        val earliestExam = blocks.minWithOrNull(
            compareBy<ReviewBlock> {
                it.examDate.takeIf { value -> value > 0L }
                    ?: runCatching { java.time.LocalDate.parse(it.date)
                        .atStartOfDay(java.time.ZoneId.of("Asia/Seoul"))
                        .toInstant()
                        .toEpochMilli()
                    }.getOrDefault(Long.MAX_VALUE)
            }.thenBy { it.blockId }
        )
        return earliestExam?.blockId?.takeIf(String::isNotBlank)?.let(::setOf).orEmpty()
    }

    fun canCreate(blocks: List<ReviewBlock>, entitlement: SubscriptionEntitlement): Boolean {
        return entitlement is SubscriptionEntitlement.Pro || blocks.isEmpty()
    }
}
