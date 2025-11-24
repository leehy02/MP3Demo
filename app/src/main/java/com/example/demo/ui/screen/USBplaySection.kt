package com.example.demo.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.demo.ble.BleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun USBplaySection(vm: BleViewModel){

    var showFileList by remember { mutableStateOf(false) }

    SettingSection(title="USB play") {
        var isPlaying by remember { mutableStateOf(false) }

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButtonStyle("Previous", Icons.Default.SkipPrevious, onClick = {vm.sendCommand(0x02, 0x04)})

            Spacer(modifier = Modifier.width(24.dp)) //  버튼 간격

            IconButtonStyle(
                content = if (isPlaying) "Pause" else "Play",
                image = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                onClick = {
                    val next = !isPlaying
                    val data = if (next) 0x01 else 0x02   // 0x01: Play, 0x02: Pause
                    vm.sendCommand(0x02, data)
                    isPlaying = next
                }
            )

            Spacer(modifier = Modifier.width(24.dp)) //  버튼 간격

            IconButtonStyle(
                content = "Next",
                image = Icons.Default.SkipNext,
                onClick = {vm.sendCommand(0x02, 0x03)}
            )

            Spacer(modifier = Modifier.width(24.dp)) //  버튼 간격

            IconButtonStyle("File Play", Icons.Default.Menu) {
                showFileList = true
            }
        }

        if (showFileList) {
            ModalBottomSheet(
                onDismissRequest = { showFileList = false }
            ) {
                FileListScreen(vm = vm)
            }
        }
    }
}

@Composable
fun IconButtonStyle(
    content: String,
    image: ImageVector,
    onClick: () -> Unit = {} // ← 기본값 추가 (선택적)
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = image,
            contentDescription = content,
            tint = Color(0xFF000000),
            modifier = Modifier.size(50.dp)
        )
    }
}

