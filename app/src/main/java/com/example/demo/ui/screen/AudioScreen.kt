package com.example.demo.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.demo.ble.BleSection
import com.example.demo.ble.BleViewModel
import com.example.demo.ui.theme.GmarketSans

@Composable
fun AudioScreen(vm: BleViewModel) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            BleSection()

            SevenSection(vm)

            MixingSection(vm)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp)   // ← 직접 지정
                ) {
                    RadioSection(vm)
                }
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .height(140.dp)   // ← 직접 지정
                ) {
                    ToggleSection(vm)
                }
            }

            VolumeSection(vm)

            USBplaySection(vm)

            EqualizerSection(vm)
        }
    }
}

@Composable
fun SettingSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFDFDFD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = Color(0xFF3D3D3D),
                fontFamily = GmarketSans,
                fontWeight = FontWeight.Bold,
                fontSize = if (title == "Volume Control") 23.sp else 18.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
            Spacer(Modifier.height(5.dp))
        }
    }
}


@Composable
fun SettingSection2(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFDFDFD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            content()
        }
    }
}