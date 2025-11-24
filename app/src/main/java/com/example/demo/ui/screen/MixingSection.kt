package com.example.demo.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.demo.ble.BleViewModel
import com.example.demo.ui.theme.GmarketSans
import kotlinx.coroutines.delay


// 확인 완료
@Composable
fun MixingSection(vm: BleViewModel) {
    SettingSection(title = "Mixing") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MixingButton(title = "Applause", onPress = { vm.sendCommand(0x70, 0x00) })
            Spacer(Modifier.width(24.dp))
            MixingButton(title = "Fanfare", onPress = { vm.sendCommand(0x70, 0x01) })
            Spacer(Modifier.width(24.dp))
            MixingButton(title = "Screaming", onPress = { vm.sendCommand(0x70, 0x02) })
            Spacer(Modifier.width(24.dp))
            MixingButton(title = "Shouting", onPress = { vm.sendCommand(0x70, 0x03) })
        }
    }
}

@Composable
fun MixingButton(
    title: String,
    color: Color = Color(0xFF00E676),
    onPress: () -> Unit = {}
) {
    var isPressed by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontFamily = GmarketSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF3D3D3D)
        )
        Spacer(Modifier.height(8.dp))

        val bg = if (isPressed) color else Color(0xFFBDBDBD)

        Surface(
            modifier = Modifier.size(width = 88.dp, height = 44.dp),
            shape = RoundedCornerShape(12.dp),
            color = bg,
            tonalElevation = if (isPressed) 6.dp else 2.dp,
            shadowElevation = if (isPressed) 3.dp else 1.dp,
            onClick = {
                isPressed = true      // 색 ON
                onPress()             // 필요한 동작 수행
            }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "",
                    fontFamily = GmarketSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }

        // 1초 후 자동 복귀
        LaunchedEffect(isPressed) {
            if (isPressed) {
                delay(1000L)
                isPressed = false
            }
        }
    }
}
