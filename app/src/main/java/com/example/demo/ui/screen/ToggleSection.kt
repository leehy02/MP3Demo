package com.example.demo.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.demo.ble.BleViewModel
import com.example.demo.ui.theme.GmarketSans

@Composable
fun ToggleSection(vm : BleViewModel) {
    var isMuteOn by remember { mutableStateOf(false) }
    var isFeedbackCancellerOn by remember { mutableStateOf(true) }

    SettingSection2 {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LabeledToggleButton(
                title = "Mute",
                isOn = isMuteOn,
                color = Color(0xFFFF3B3B),
                onToggle = {
                    val next = !isMuteOn
                    // OFF -> ON 이면 0x01, ON -> OFF 이면 0x00
                    val data = if (next) 0x01 else 0x00
                    vm.sendCommand(0x20, data)
                    isMuteOn = next
                }
            )
            Spacer(modifier = Modifier.width(24.dp)) // 두 버튼 간 간격
            LabeledToggleButton(
                title = "FdBack Cancel",
                isOn = isFeedbackCancellerOn,
                color = Color(0xFF00E676),
                onToggle = {
                    val next = !isFeedbackCancellerOn
                    // OFF -> ON 이면 0x01, ON -> OFF 이면 0x00
                    val data = if (next) 0x01 else 0x00
                    vm.sendCommand(0xC0, data)
                    isFeedbackCancellerOn = next
                }
            )
        }
    }

}

@Composable
fun LabeledToggleButton(
    title: String,
    isOn: Boolean,
    color: Color,
    onToggle: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 🔹 각 버튼 위에 개별 타이틀 표시
        Text(
            text = title,
            fontFamily = GmarketSans,
            fontWeight = FontWeight.Bold,
            fontSize = if (title == "FdBack Cancel") 13.sp else 16.sp,
            color = Color(0xFF3D3D3D)
        )
        Spacer(Modifier.height(8.dp))

        val bg = if (isOn) color else Color(0xFFBDBDBD) // ON=초록, OFF=회색

        Surface(
            modifier = Modifier
                .size(width = 88.dp, height = 44.dp)
                .clickable { onToggle() },
            shape = RoundedCornerShape(12.dp),
            color = bg,
            tonalElevation = if (isOn) 6.dp else 2.dp,
            shadowElevation = if (isOn) 3.dp else 1.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = if (isOn) "ON" else "OFF",
                    fontFamily = GmarketSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}
