package com.loorve.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.loorve.ui.theme.*

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .background(Primary)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = LoorveTypography.titleMedium,
            color = OnBackground
        )
    }
}