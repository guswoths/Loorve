package com.loorve.domain.subscription

import com.loorve.domain.model.ReviewBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewBlockAccessPolicyTest {
    private fun block(id: String, createdAt: Long) = ReviewBlock(
        blockId = id,
        createdAt = createdAt
    )

    @Test
    fun freeUserCanCreateFirstBlock() {
        assertTrue(
            ReviewBlockAccessPolicy.canCreate(
                emptyList(),
                SubscriptionEntitlement.Free
            )
        )
    }

    @Test
    fun freeUserCanAccessOnlyNewestBlock() {
        val blocks = listOf(block("old", 1L), block("new", 2L))

        assertEquals(
            setOf("new"),
            ReviewBlockAccessPolicy.accessibleBlockIds(
                blocks,
                SubscriptionEntitlement.Free
            )
        )
    }

    @Test
    fun proUserCanCreateAndAccessAllBlocks() {
        val blocks = listOf(block("old", 1L), block("new", 2L))

        assertTrue(
            !ReviewBlockAccessPolicy.canCreate(
                blocks,
                SubscriptionEntitlement.Free
            )
        )
        assertTrue(
            ReviewBlockAccessPolicy.canCreate(
                blocks,
                SubscriptionEntitlement.Pro
            )
        )
        assertEquals(
            setOf("old", "new"),
            ReviewBlockAccessPolicy.accessibleBlockIds(
                blocks,
                SubscriptionEntitlement.Pro
            )
        )
    }

    @Test
    fun missingCreationTimestampUsesPreparationStartDate() {
        val blocks = listOf(
            block("old", 0L).copy(prepStartDate = 10L),
            block("new", 0L).copy(prepStartDate = 20L)
        )

        assertEquals(
            setOf("new"),
            ReviewBlockAccessPolicy.accessibleBlockIds(
                blocks,
                SubscriptionEntitlement.Free
            )
        )
    }
}
