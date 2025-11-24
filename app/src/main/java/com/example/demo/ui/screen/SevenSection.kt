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
fun SevenSection(vm: BleViewModel){
    // 1.Vocal Separation - 좌우 화살표 필요
    SettingSection(title = "Vocal Separation") {
        val options = listOf(
            "Voice 100%", "60%", "30%", "Off",
            "30%", "60%", "Music 100%"
        )
        var selected by remember { mutableStateOf(3) }

        val dataMap = intArrayOf(0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13)

        DiscreteStepperWithLabels(
            items = options,
            selectedIndex = selected,
            onChange = { idx ->
                val safe = idx.coerceIn(0, options.lastIndex)
                if (safe != selected) {                 // 같은 위치 재전송 방지(원하면 제거)
                    selected = safe
                    val data = dataMap[safe]
                    vm.sendCommand(0x50, data)   // EC ID DATA CS
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // 2.Key Changer - 좌우 화살표 필요
    SettingSection(title = "Key Changer") {
        val options = listOf(
            "b4", "b3", "b2", "b1", "Off", "#1", "#2", "#3", "#4"
        )
        var selected by remember { mutableStateOf(4) }

        val dataMap = intArrayOf(0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14)

        DiscreteStepperWithLabels(
            items = options,
            selectedIndex = selected,
            onChange = { idx ->
                val safe = idx.coerceIn(0, options.lastIndex)
                if (safe != selected) {                 // 같은 위치 재전송 방지(원하면 제거)
                    selected = safe
                    val data = dataMap[safe]
                    vm.sendCommand(0x51, data)   // EC ID DATA CS
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // 3.DJ Effect
    SettingSection(title = "DJ Effect") {
        val options = listOf(
            "Off", "Flanger", "Phaser", "Reverb", "Wah"
        )
        val dataMap = intArrayOf(0x00, 0x01, 0x02, 0x03, 0x04)
        var selected by remember { mutableStateOf(0) }

        SlideStepperWithLabels(
            items = options,
            selectedIndex = selected,
            onChange = { idx ->
                val safe = idx.coerceIn(0, options.lastIndex)
                if (safe != selected) {            // 같은 인덱스 중복 전송 방지
                    selected = safe
                    vm.sendCommand(0x52, dataMap[safe]) // EC 53 data checksum
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // 4.DJ Effect Strength
    SettingSection(title = "DJ Effect Strength") {
        val options = listOf("Low", "Mid", "High")
        val dataMap = intArrayOf(0x01, 0x02, 0x03)
        var selected by remember { mutableStateOf(1) } // 기본 Mid

        SlideStepperWithLabels(
            items = options,
            selectedIndex = selected,
            onChange = { idx ->
                val safe = idx.coerceIn(0, options.lastIndex)
                if (safe != selected) {            // 같은 인덱스 중복 전송 방지
                    selected = safe
                    vm.sendCommand(0x53, dataMap[safe]) // EC 53 data checksum
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // 5.Bass Booster
    SettingSection(title = "Bass Booster") {
        val options = listOf(
            "Off", "Low", "Mid", "High"
        )
        val dataMap = intArrayOf(0x00, 0x01, 0x02, 0x03)
        var selected by remember { mutableStateOf(2) }

        SlideStepperWithLabels(
            items = options,
            selectedIndex = selected,
            onChange = { idx ->
                val safe = idx.coerceIn(0, options.lastIndex)
                if (safe != selected) {            // 같은 인덱스 중복 전송 방지
                    selected = safe
                    vm.sendCommand(0x54, dataMap[safe]) // EC 53 data checksum
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // 6.Mic Reverberation
    SettingSection(title = "Mic Reverberation") {
        val options = listOf(
            "Off", "Low", "Mid", "High"
        )
        val dataMap = intArrayOf(0x00, 0x01, 0x02, 0x03)

        var selected by remember { mutableStateOf(2) }

        SlideStepperWithLabels(
            items = options,
            selectedIndex = selected,
            onChange = { idx ->
                val safe = idx.coerceIn(0, options.lastIndex)
                if (safe != selected) {            // 같은 인덱스 중복 전송 방지
                    selected = safe
                    vm.sendCommand(0x54, dataMap[safe]) // EC 53 data checksum
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // 7.Vocal Effect
    SettingSection(title = "Vocal Effect") {
        val options = listOf(
            "Off", "Minions",  "Monster", "Helium", "Robot", "Duet"
        )
        val dataMap = intArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05)

        var selected by remember { mutableStateOf(0) }

        SlideStepperWithLabels(
            items = options,
            selectedIndex = selected,
            onChange = { idx ->
                val safe = idx.coerceIn(0, options.lastIndex)
                if (safe != selected) {            // 같은 인덱스 중복 전송 방지
                    selected = safe
                    vm.sendCommand(0x54, dataMap[safe]) // EC 53 data checksum
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}