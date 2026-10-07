package io.ethan.pushgo.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Generic structured task data rendered using the application's original Material components. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskMessageCard(task: NativeTaskCard) {
    val zone = runCatching { ZoneId.of(task.timezone) }.getOrDefault(ZoneId.of("Asia/Shanghai"))
    fun date(value: String?): String = runCatching { DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm").withZone(zone).format(Instant.parse(value)) }.getOrDefault("未设置")
    Card(modifier = Modifier.fillMaxWidth().testTag("section.message.task_card"), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        SelectionContainer { Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("任务标题", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(task.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            TaskField("任务内容", task.content.ifBlank { "暂无任务内容" })
            TaskField("开始时间", date(task.start))
            TaskField("截止时间", date(task.due))
            TaskField("任务状态", task.status)
            TaskField("优先级", task.priority)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("任务标签", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (task.tags.isEmpty()) Text("未添加标签", style = MaterialTheme.typography.bodyMedium)
                else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { task.tags.forEach { tag ->
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) { Text(tag.removePrefix("#"), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium) }
                } }
            }
            TaskField("任务编号", task.number)
            TaskField("创建时间", date(task.created))
            TaskField("时区", if (task.timezone == "Asia/Shanghai") "北京时间" else task.timezone)
            TaskField("任务来源", task.source)
        } }
    }
}
@Composable
private fun TaskField(label: String, value: String) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodyMedium) } }
