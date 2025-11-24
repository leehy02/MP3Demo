package com.example.demo.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.demo.ble.BleViewModel

@Composable
fun VolumeSection(vm: BleViewModel){
    SettingSection(title= "Volume Control") {
        //  Master Volume
        SettingSection(title = "Master Volume") {
            val options = listOf(
                "0", "1",  "2", "3", "4", "5", "6", "7", "8", "9", "10"
            )
            var selected by remember { mutableStateOf(5) }

            val dataMap = intArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A)

            VolumeStepper(
                items = options,
                selectedIndex = selected,
                onChange = { idx ->
                    val safe = idx.coerceIn(0, options.lastIndex)
                    if (safe != selected) {                 // 같은 위치 재전송 방지(원하면 제거)
                        selected = safe
                        val data = dataMap[safe]
                        vm.sendCommand(0x10, data)   // EC ID DATA CS
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        //   Music Volume
        SettingSection(title = "Music Volume") {
            val options = listOf(
                "0", "1",  "2", "3", "4", "5", "6", "7", "8", "9", "10"
            )
            var selected by remember { mutableStateOf(10) }

            val dataMap = intArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A)

            VolumeStepper(
                items = options,
                selectedIndex = selected,
                onChange = { idx ->
                    val safe = idx.coerceIn(0, options.lastIndex)
                    if (safe != selected) {                 // 같은 위치 재전송 방지(원하면 제거)
                        selected = safe
                        val data = dataMap[safe]
                        vm.sendCommand(0x11, data)   // EC ID DATA CS
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        //   Mic Volume
        SettingSection(title = "Mic Volume") {
            val options = listOf(
                "0", "1",  "2", "3", "4", "5", "6", "7", "8", "9", "10"
            )
            var selected by remember { mutableStateOf(10) }

            val dataMap = intArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A)

            VolumeStepper(
                items = options,
                selectedIndex = selected,
                onChange = { idx ->
                    val safe = idx.coerceIn(0, options.lastIndex)
                    if (safe != selected) {                 // 같은 위치 재전송 방지(원하면 제거)
                        selected = safe
                        val data = dataMap[safe]
                        vm.sendCommand(0x12, data)   // EC ID DATA CS
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}