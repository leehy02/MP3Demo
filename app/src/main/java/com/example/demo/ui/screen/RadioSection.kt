package com.example.demo.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.demo.ui.theme.GmarketSans
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.demo.ble.BleViewModel

enum class AudioMode(val label: String) {
    Bluetooth("Bluetooth"),
    USB("USB"),
    LineIn("Line In")
}

@Composable
fun RadioSection(vm : BleViewModel) {
    var selected by remember { mutableStateOf(AudioMode.Bluetooth) }

    // ✅ SettingSection 카드 스타일 유지
    SettingSection(title = "Mode") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModeOptionChip(
                text = AudioMode.Bluetooth.label,
                selected = (selected == AudioMode.Bluetooth),
                onClick = {
                    selected = AudioMode.Bluetooth
                    vm.sendCommand(0x01, 0x00)
                },
            )
            ModeOptionChip(
                text = AudioMode.USB.label,
                selected = (selected == AudioMode.USB),
                onClick = {
                    selected = AudioMode.USB
                    vm.sendCommand(0x01, 0x01)
                }
            )
            ModeOptionChip(
                text = AudioMode.LineIn.label,
                selected = (selected == AudioMode.LineIn),
                onClick = {
                    selected = AudioMode.LineIn
                    vm.sendCommand(0x01, 0x02)
                }
            )
        }
    }
}

@Composable
private fun ModeOptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFFE8F0FF) else Color(0xFFF7F7F7)
    val txt = if (selected) Color(0xFF2F6FD6) else Color(0xFF3D3D3D)
    val shape = RoundedCornerShape(14.dp)

    Surface(
        modifier = Modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        shape = shape,
        color = bg,
        tonalElevation = if (selected) 6.dp else 2.dp,   // 입체감
        shadowElevation = if (selected) 3.dp else 1.dp   // 살짝 그림자
    ) {
        Box(
            modifier = Modifier
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontFamily = GmarketSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = txt
            )
        }
    }
}
