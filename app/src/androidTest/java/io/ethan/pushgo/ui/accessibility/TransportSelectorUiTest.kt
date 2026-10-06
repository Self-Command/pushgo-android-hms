package io.ethan.pushgo.ui.accessibility

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.ethan.pushgo.data.PushChannelType
import io.ethan.pushgo.ui.screens.TransportSelectorRow
import io.ethan.pushgo.ui.theme.PushGoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransportSelectorUiTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test fun compactSelectorKeepsAllThreeOptionsInsideOneRow() {
        composeRule.setContent {
            PushGoTheme {
                Box(Modifier.requiredWidth(320.dp)) {
                    TransportSelectorRow(
                        icon = Icons.Default.Search, title = "Transport", subtitle = null,
                        selectedChannel = PushChannelType.PRIVATE, isFcmSupported = true,
                        isPrivateSupported = true, isHmsSupported = true, hmsConfigured = true,
                        isSwitching = false, onSelectChannel = {},
                    )
                }
            }
        }
        val row = composeRule.onNodeWithTag("segmented.settings.notification_transport").fetchSemanticsNode().boundsInRoot
        val buttons = listOf("fcm", "private", "hms").map {
            composeRule.onNodeWithTag("option.settings.notification_transport.$it").fetchSemanticsNode().boundsInRoot
        }
        for (button in buttons) {
            assertTrue(button.left >= row.left - 1 && button.right <= row.right + 1)
            assertTrue(button.top >= row.top - 1 && button.bottom <= row.bottom + 1)
            assertEquals(buttons.first().top, button.top, 1f)
            assertEquals(buttons.first().bottom, button.bottom, 1f)
        }
        composeRule.onNodeWithText("HMS").assertIsDisplayed()
    }

    @Test fun pendingSwitchShowsFeedbackAndBlocksDuplicateClicks() {
        val switching = mutableStateOf(false)
        var calls = 0
        composeRule.setContent {
            PushGoTheme {
                TransportSelectorRow(
                    icon = Icons.Default.Search, title = "Transport", subtitle = null,
                    selectedChannel = PushChannelType.PRIVATE, isFcmSupported = true,
                    isPrivateSupported = true, isHmsSupported = true, hmsConfigured = true,
                    isSwitching = switching.value, onSelectChannel = { calls++; switching.value = true },
                )
            }
        }
        composeRule.onNodeWithTag("option.settings.notification_transport.hms").performClick()
        composeRule.onNodeWithTag("progress.settings.notification_transport").assertIsDisplayed()
        for (type in listOf("fcm", "private", "hms")) {
            composeRule.onNodeWithTag("option.settings.notification_transport.$type").assertIsNotEnabled()
        }
        composeRule.runOnIdle { assertEquals(1, calls) }
    }
}
