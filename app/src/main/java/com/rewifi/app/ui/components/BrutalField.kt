package com.rewifi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Muted
import com.rewifi.app.ui.theme.Snow

/**
 * Brutalist labelled text field. When [isPassword] is true it masks the text and
 * shows a tap-to-reveal eye icon so the user can double-check what they typed.
 */
@Composable
fun BrutalField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    val masked = isPassword && !visible

    Column(modifier) {
        Text(label, fontWeight = FontWeight.Black, fontSize = 12.sp, color = Muted, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth()
                .background(Snow, RoundedCornerShape(12.dp))
                .border(3.dp, Ink, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(hint, color = Muted, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Ink, fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                    ),
                    visualTransformation =
                        if (masked) PasswordVisualTransformation() else VisualTransformation.None,
                    cursorBrush = SolidColor(Ink),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (isPassword) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Hide" else "Show",
                    tint = Ink,
                    modifier = Modifier.size(24.dp).clickable { visible = !visible }
                )
            }
        }
    }
}
