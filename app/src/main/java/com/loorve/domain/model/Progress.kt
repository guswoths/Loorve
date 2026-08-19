// After (권장 확장안)
data class Progress(
    val id: String = "",
    val examId: String = "",       // 연결된 시험 ID (과목 식별자로 활용)
    val content: String = "",
    val completedCount: Int = 0,   // 완료한 학습 항목 수
    val totalCount: Int = 0,       // 전체 학습 항목 수
    val isCompleted: Boolean = false, // 전체 완료 여부 플래그
    val createdAt: Long = 0L
)
