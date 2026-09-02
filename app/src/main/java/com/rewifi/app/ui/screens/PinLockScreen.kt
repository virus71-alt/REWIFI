package com.rewifi.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Red
import com.rewifi.app.ui.theme.RewifiTheme
import com.rewifi.app.ui.theme.Snow
import com.rewifi.app.ui.theme.Yellow
import kotlinx.coroutines.delay

@Composable
fun PinLockScreen(
    pinLength: Int,
    allowBiometric: Boolean,
    onVerifyPin: (String) -> Boolean,
    onRecordFailedAttempt: () -> Long,
    getRemainingLockoutSeconds: () -> Int,
    onBiometricClick: () -> Unit,
    onSuccess: () -> Unit
) {
    val colors = RewifiTheme.colors
    var enteredDigits by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableStateOf(getRemainingLockoutSeconds()) }

    // Lockout countdown timer
    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds = getRemainingLockoutSeconds()
        }
    }

    // Auto-trigger biometric on initial launch if allowed and not locked out
    LaunchedEffect(Unit) {
        if (allowBiometric && lockoutSeconds <= 0) {
            onBiometricClick()
        }
    }

    fun handleDigit(d: String) {
        if (lockoutSeconds > 0) return
        if (enteredDigits.length < pinLength) {
            val updated = enteredDigits + d
            enteredDigits = updated
            errorMsg = null
            if (updated.length == pinLength) {
                val ok = onVerifyPin(updated)
                if (ok) {
                    onSuccess()
                } else {
                    onRecordFailedAttempt()
                    lockoutSeconds = getRemainingLockoutSeconds()
                    enteredDigits = ""
                    errorMsg = if (lockoutSeconds > 0) {
                        "TOO MANY FAILED ATTEMPTS"
                    } else {
                        "INCORRECT PIN"
                    }
                }
            }
        }
    }

    fun handleDelete() {
        if (lockoutSeconds > 0) return
        if (enteredDigits.isNotEmpty()) {
            enteredDigits = enteredDigits.dropLast(1)
            errorMsg = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Yellow)
                        .border(3.dp, colors.border, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "ENTER REWIFI PIN",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = colors.textPrimary,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(6.dp))

                if (lockoutSeconds > 0) {
                    Text(
                        "LOCKOUT ACTIVE: RETRY IN ${lockoutSeconds}s",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Red
                    )
                } else {
                    Text(
                        "Enter your $pinLength-digit security PIN",
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(24.dp))

                // PIN Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until pinLength) {
                        val isFilled = i < enteredDigits.length
                        val isError = errorMsg != null && enteredDigits.isEmpty()
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isError -> Red.copy(alpha = 0.3f)
                                        isFilled -> Yellow
                                        else -> colors.surfaceVariant
                                    }
                                )
                                .border(
                                    2.5.dp,
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

                // Error message display
                Spacer(Modifier.height(12.dp))
                AnimatedVisibility(
                    visible = errorMsg != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        errorMsg ?: "",
                        color = Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Numeric Keypad (3x4)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isLockedOut = lockoutSeconds > 0

                // Row 1
                KeypadRow(listOf("1", "2", "3"), isLockedOut) { handleDigit(it) }
                // Row 2
                KeypadRow(listOf("4", "5", "6"), isLockedOut) { handleDigit(it) }
                // Row 3
                KeypadRow(listOf("7", "8", "9"), isLockedOut) { handleDigit(it) }

                // Row 4: Biometric, 0, Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    // Biometric button (or empty space)
                    if (allowBiometric) {
                        Box(
                            Modifier
                                .size(width = 80.dp, height = 58.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isLockedOut) colors.surfaceVariant.copy(alpha = 0.5f) else Yellow)
                                .border(2.5.dp, colors.border, RoundedCornerShape(14.dp))
                                .clickable(enabled = !isLockedOut) { onBiometricClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = "Biometric Unlock",
                                tint = Ink,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
                        Spacer(Modifier.size(width = 80.dp, height = 58.dp))
                    }

                    // Digit 0
                    KeypadButton(
                        text = "0",
                        enabled = !isLockedOut,
                        onClick = { handleDigit("0") }
                    )

                    // Backspace button
                    Box(
                        Modifier
                            .size(width = 80.dp, height = 58.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surfaceVariant)
                            .border(2.5.dp, colors.border, RoundedCornerShape(14.dp))
                            .clickable(enabled = !isLockedOut && enteredDigits.isNotEmpty()) { handleDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Backspace,
                            contentDescription = "Delete",
                            tint = if (enteredDigits.isNotEmpty() && !isLockedOut) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadRow(
    digits: List<String>,
    isLockedOut: Boolean,
    onDigitClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
    ) {
        digits.forEach { d ->
            KeypadButton(
                text = d,
                enabled = !isLockedOut,
                onClick = { onDigitClick(d) }
            )
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = RewifiTheme.colors
    Box(
        Modifier
            .size(width = 80.dp, height = 58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) colors.surface else colors.surfaceVariant.copy(alpha = 0.5f))
            .border(2.5.dp, colors.border, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            color = if (enabled) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f)
        )
    }
}
