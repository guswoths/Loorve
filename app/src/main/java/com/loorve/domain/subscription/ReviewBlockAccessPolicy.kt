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

        val newest = blocks.maxWithOrNull(
            compareBy<ReviewBlock> { it.createdAt.takeIf { value -> value > 0L } ?: it.prepStartDate }
                .thenBy { it.blockId }
        )
        return newest?.blockId?.takeIf(String::isNotBlank)?.let(::setOf).orEmpty()
    }

    fun canCreate(blocks: List<ReviewBlock>, entitlement: SubscriptionEntitlement): Boolean {
        return entitlement is SubscriptionEntitlement.Pro || blocks.isEmpty()
    }
}
