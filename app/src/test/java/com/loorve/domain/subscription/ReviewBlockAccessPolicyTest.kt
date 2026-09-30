package com.loorve.domain.subscription

import com.loorve.domain.model.ReviewBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReviewBlockAccessPolicyTest {
    private val seoulZone = ZoneId.of("Asia/Seoul")

    private fun block(id: String, createdAt: Long, examDate: LocalDate? = null) = ReviewBlock(
        blockId = id,
        createdAt = createdAt,
        examDate = examDate?.atStartOfDay(seoulZone)?.toInstant()?.toEpochMilli() ?: 0L
    )

    @Test
    fun freeUserCanCreateUpToTwoBlocks() {
        assertTrue(
            ReviewBlockAccessPolicy.canCreate(
                emptyList(),
                SubscriptionEntitlement.Free
            )
        )
        assertTrue(
            ReviewBlockAccessPolicy.canCreate(
                listOf(block("first", 1L)),
                SubscriptionEntitlement.Free
            )
        )
        assertTrue(
            !ReviewBlockAccessPolicy.canCreate(
                listOf(block("first", 1L), block("second", 2L)),
                SubscriptionEntitlement.Free
            )
        )
    }

    @Test
    fun freeUserCanAccessUpToTwoBlocksWithEarliestUpcomingExam() {
        val blocks = listOf(
            block("latest", 1L, LocalDate.of(2099, 1, 3)),
            block("middle", 2L, LocalDate.of(2099, 1, 2)),
            block("earliest", 3L, LocalDate.of(2099, 1, 1))
        )

        assertEquals(
            setOf("earliest", "middle"),
            ReviewBlockAccessPolicy.accessibleBlockIds(
                blocks,
                SubscriptionEntitlement.Free
            )
        )
    }

    @Test
    fun proUserCanCreateAndAccessAllBlocks() {
        val blocks = listOf(block("old", 1L), block("new", 2L), block("third", 3L))

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
            setOf("old", "new", "third"),
            ReviewBlockAccessPolicy.accessibleBlockIds(
                blocks,
                SubscriptionEntitlement.Pro
            )
        )
    }

    @Test
    fun freeUserCanAccessEarliestUpcomingBlockBeforeEndedBlocks() {
        val blocks = listOf(
            block("ended", 1L, LocalDate.of(2020, 1, 1)),
            block("upcoming-1", 2L, LocalDate.of(2099, 1, 1)),
            block("upcoming-2", 3L, LocalDate.of(2099, 1, 2))
        )

        assertEquals(
            setOf("upcoming-1", "upcoming-2"),
            ReviewBlockAccessPolicy.accessibleBlockIds(
                blocks,
                SubscriptionEntitlement.Free
            )
        )
    }

    @Test
    fun endedBlocksAreSortedAfterUpcomingBlocks() {
        val blocks = listOf(
            block("ended", 1L, LocalDate.of(2020, 1, 1)),
            block("upcoming-later", 2L, LocalDate.of(2099, 1, 2)),
            block("upcoming-earlier", 3L, LocalDate.of(2099, 1, 1))
        )

        assertEquals(
            listOf("upcoming-earlier", "upcoming-later", "ended"),
            ReviewBlockAccessPolicy.sortBlocks(
                blocks,
                today = LocalDate.of(2025, 1, 1)
            ).map { it.blockId }
        )
    }
}
