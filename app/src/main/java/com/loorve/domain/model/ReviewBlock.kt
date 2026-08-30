package com.loorve.domain.model

/**
 * 특정 날짜에 표시할 복습 블록입니다.
 * Firestore 역직렬화를 위해 모든 필드에 기본값을 지정합니다.
 *
 * ⚠️ @DocumentId 제거:
 * 하위 컬렉션(users/{uid}/reviewBlocks/)에서 toObjects() 역직렬화 시
 * "blockId was found from document" 충돌이 발생하므로 제거.
 * blockId는 Repository에서 snapshot.id로 수동 할당합니다.
 */
data class ReviewBlock(
    val blockId: String = "",
    val uid: String = "",
    val date: String = "",
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)