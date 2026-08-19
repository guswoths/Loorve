// 경로: app/src/main/java/com/loorve/presentation/home/ProgressInputSection.kt
package com.loorve.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loorve.domain.model.Exam

/**
 * 오늘의 학습 진도 입력 섹션.
 *
 * @param exams 선택 가능한 시험 목록
 * @param onSave 저장 버튼 클릭 콜백 (id, content, completedCount, totalCount)
 * @param modifier 외부에서 주입 가능한 Modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressInputSection(
    exams: List<Exam>,
    // ✅ 수정: examId → id 로 파라미터명 통일 (caller인 HomeScreen과 일치)
    onSave: (id: String, content: String, completedCount: Int, totalCount: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedExam by remember { mutableStateOf<Exam?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var content by remember { mutableStateOf("") }
    var completedCountText by remember { mutableStateOf("") }
    var totalCountText by remember { mutableStateOf("") }

    val isSaveEnabled = selectedExam != null && content.isNotBlank()

    Card(
        modifier  = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text  = "오늘의 학습 진도 입력",
                style = MaterialTheme.typography.titleMedium
            )

            ExposedDropdownMenuBox(
                expanded         = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value            = selectedExam?.subjectName ?: "",
                    onValueChange    = {},
                    readOnly         = true,
                    label            = { Text("시험 선택") },
                    trailingIcon     = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    colors           = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier         = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded         = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    if (exams.isEmpty()) {
                        DropdownMenuItem(
                            text    = { Text("등록된 시험이 없습니다.") },
                            onClick = { dropdownExpanded = false },
                            enabled = false
                        )
                    } else {
                        exams.forEach { exam ->
                            DropdownMenuItem(
                                text    = { Text(exam.subjectName) },
                                onClick = {
                                    selectedExam     = exam
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value         = content,
                onValueChange = { content = it },
                label         = { Text("학습 내용") },
                placeholder   = { Text("오늘 학습한 내용을 입력하세요") },
                singleLine    = false,
                maxLines      = 3,
                modifier      = Modifier.fillMaxWidth()
            )

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value           = completedCountText,
                    onValueChange   = { if (it.length <= 5 && it.all(Char::isDigit)) completedCountText = it },
                    label           = { Text("완료 수") },
                    placeholder     = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine      = true,
                    modifier        = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value           = totalCountText,
                    onValueChange   = { if (it.length <= 5 && it.all(Char::isDigit)) totalCountText = it },
                    label           = { Text("전체 수") },
                    placeholder     = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine      = true,
                    modifier        = Modifier.weight(1f)
                )
            }

            Button(
                onClick  = {
                    // ✅ 수정: examId → id (Exam 모델의 실제 필드명 및 onSave 시그니처와 일치)
                    val id        = selectedExam?.id ?: return@Button
                    val completed = completedCountText.toIntOrNull() ?: 0
                    val total     = totalCountText.toIntOrNull() ?: 0
                    onSave(id, content.trim(), completed, total)
                    content            = ""
                    completedCountText = ""
                    totalCountText     = ""
                    selectedExam       = null
                },
                enabled  = isSaveEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("저장")
            }
        }
    }
}

// ── Preview ────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "ProgressInputSection - 시험 있음")
@Composable
private fun ProgressInputSectionPreview() {
    MaterialTheme {
        val sampleExams = listOf(
            Exam(id = "1", subjectName = "정보처리기사", examDate = 0L),
            Exam(id = "2", subjectName = "SQLD", examDate = 0L)
        )
        ProgressInputSection(
            exams  = sampleExams,
            onSave = { _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "ProgressInputSection - 시험 없음")
@Composable
private fun ProgressInputSectionEmptyPreview() {
    MaterialTheme {
        ProgressInputSection(
            exams  = emptyList(),
            onSave = { _, _, _, _ -> }
        )
    }
}
