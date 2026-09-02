package com.rewifi.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Red
import com.rewifi.app.ui.theme.RewifiTheme
import com.rewifi.app.ui.theme.Yellow

enum class PinSetupMode {
    CREATE,
    CHANGE,
    VERIFY_TO_DISABLE
}

@Composable
fun PinSetupDialog(
    mode: PinSetupMode,
    currentPinLength: Int = 4,
    onVerifyCurrentPin: (String) -> Boolean = { false },
    onPinConfirmed: (pin: String, length: Int) -> Unit,
    onVerifiedToDisable: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val colors = RewifiTheme.colors

    // Steps:
    // 0: VERIFY_CURRENT (if mode == CHANGE or VERIFY_TO_DISABLE)
    // 1: ENTER_NEW
    // 2: CONFIRM_NEW
    var currentStep by remember {
        mutableIntStateOf(if (mode == PinSetupMode.CREATE) 1 else 0)
    }

    var selectedLength by remember { mutableIntStateOf(currentPinLength) }
    var enteredDigits by remember { mutableStateOf("") }
    var stagedNewPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val activeTargetLength = if (currentStep == 0) currentPinLength else selectedLength

    val title = when (currentStep) {
        0 -> if (mode == PinSetupMode.VERIFY_TO_DISABLE) "DISABLE APP LOCK" else "VERIFY CURRENT PIN"
        1 -> if (mode == PinSetupMode.CHANGE) "ENTER NEW PIN" else "SET REWIFI PIN"
        else -> "CONFIRM NEW PIN"
    }

    val subtitle = when (currentStep) {
        0 -> "Enter your current PIN to proceed"
        1 -> "Enter a $selectedLength-digit PIN"
        else -> "Re-enter the $selectedLength-digit PIN"
    }

    fun handleDigit(d: String) {
        if (enteredDigits.length < activeTargetLength) {
            val updated = enteredDigits + d
            enteredDigits = updated
            errorMsg = null

            if (updated.length == activeTargetLength) {
                when (currentStep) {
                    0 -> {
                        // Verify current
                        val valid = onVerifyCurrentPin(updated)
                        if (valid) {
                            if (mode == PinSetupMode.VERIFY_TO_DISABLE) {
                                onVerifiedToDisable()
                            } else {
                                currentStep = 1
                                enteredDigits = ""
                                errorMsg = null
                            }
                        } else {
                            enteredDigits = ""
                            errorMsg = "CURRENT PIN INCORRECT"
                        }
                    }
                    1 -> {
                        // Staged new PIN
                        stagedNewPin = updated
                        enteredDigits = ""
                        currentStep = 2
                    }
                    2 -> {
                        // Confirm new PIN
                        if (updated == stagedNewPin) {
                            onPinConfirmed(updated, selectedLength)
                        } else {
                            enteredDigits = ""
                            errorMsg = "PINS DO NOT MATCH. RETRY"
                        }
                    }
                }
            }
        }
    }

    fun handleDelete() {
        if (enteredDigits.isNotEmpty()) {
            enteredDigits = enteredDigits.dropLast(1)
            errorMsg = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            BrutalCard(
                Modifier.fillMaxWidth(),
                padding = PaddingValues(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header icon & titles
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Yellow)
                            .border(2.5.dp, colors.border, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Ink, modifier = Modifier.size(24.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            title,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = colors.textPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            subtitle,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }

                    // Length selector (only in Step 1)
                    if (currentStep == 1) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(4, 6).forEach { len ->
                                val selected = selectedLength == len
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Yellow else colors.surfaceVariant)
                                        .border(2.dp, colors.border, RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (selectedLength != len) {
                                                selectedLength = len
                                                enteredDigits = ""
                                                errorMsg = null
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "$len DIGITS",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = if (selected) Ink else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until activeTargetLength) {
                            val isFilled = i < enteredDigits.length
                            val isError = errorMsg != null && enteredDigits.isEmpty()
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isError -> Red.copy(alpha = 0.3f)
                                            isFilled -> Yellow
                                            else -> colors.surfaceVariant
                                        }
                                    )
                                    .border(
                                        2.dp,
                                        when {
                                            isError -> Red
                                            isFilled -> colors.border
                                            else -> colors.border.copy(alpha = 0.4f)
                                        },
                                        CircleShape
                                    )
                            )
                        }
                    }

                    // Error text
                    AnimatedVisibility(
                        visible = errorMsg != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            errorMsg ?: "",
                            color = Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Numeric Keypad
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9")
                        ).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                            ) {
                                row.forEach { d ->
                                    DialogKeypadBtn(d) { handleDigit(d) }
                                }
                            }
                        }

                        // Bottom Row: Empty, 0, Backspace
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                        ) {
                            Spacer(Modifier.size(width = 68.dp, height = 48.dp))

                            DialogKeypadBtn("0") { handleDigit("0") }

                            Box(
                                Modifier
                                    .size(width = 68.dp, height = 48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surfaceVariant)
                                    .border(2.dp, colors.border, RoundedCornerShape(10.dp))
                                    .clickable(enabled = enteredDigits.isNotEmpty()) { handleDelete() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Backspace,
                                    contentDescription = "Delete",
                                    tint = if (enteredDigits.isNotEmpty()) colors.textPrimary else colors.textSecondary.copy(alpha = 0.3f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Cancel button
                    BrutalButton(
                        text = "CANCEL",
                        modifier = Modifier.fillMaxWidth(),
                        bg = colors.surface,
                        fg = colors.textSecondary,
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogKeypadBtn(text: String, onClick: () -> Unit) {
    val colors = RewifiTheme.colors
    Box(
        Modifier
            .size(width = 68.dp, height = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .border(2.dp, colors.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = colors.textPrimary
        )
    }
}
