package io.ethan.pushgo.web
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.ethan.pushgo.ui.theme.PushGoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class TaskMessageCardUiTest {
 @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
 @Test fun chineseTaskFieldsAndContentUseNativeCard() {
  rule.setContent { PushGoTheme { Column(Modifier.verticalScroll(rememberScrollState())) { TaskMessageCard(NativeTaskCard("整理读书笔记", "阅读第五章，整理三个要点。", "2026-10-07T01:00:00Z", "2026-10-07T02:00:00Z", "高", "进行中", listOf("学习", "打卡"), "Obsidian 任务", "Asia/Shanghai")) } } }
  rule.onNodeWithText("整理读书笔记").assertIsDisplayed()
  for (field in listOf("任务内容", "开始时间", "截止时间", "任务状态", "优先级", "任务标签", "任务来源")) rule.onNodeWithText(field).performScrollTo().assertIsDisplayed()
  rule.onNodeWithText("阅读第五章，整理三个要点。").performScrollTo().assertIsDisplayed()
  rule.onNodeWithText("2026年10月7日 09:00").performScrollTo().assertIsDisplayed()
  rule.onNodeWithText("task_card_version").assertDoesNotExist()
 }
}
