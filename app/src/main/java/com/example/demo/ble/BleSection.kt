package com.example.demo.ble

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.demo.ui.screen.SettingSection

@Composable
fun BleSection() {
    val context = LocalContext.current
    val vm: BleViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return BleViewModel(context) as T
            }
        }
    )

    val scanState by vm.scanState.collectAsState()
    val devices by vm.devices.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var autoCloseTimer by remember { mutableStateOf(false) }

    //----------------------권한 요청 섹션----------------------
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { result ->
            val allGranted = result.all { it.value }
            if (allGranted) {
                Log.d("BLE_test", "✅ BLE 권한 모두 허용됨")
                Toast.makeText(context, "BLE 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Log.w("BLE_test", "❌ 일부 권한이 거부됨: $result")
                Toast.makeText(context, "일부 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    )
    //----------------------권한 요청 섹션----------------------

    SettingSection(title = "Bluetooth 연결하기") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1️⃣ 권한 요청
            Button(
                onClick = {
                    if (hasBlePermissions(context)) {
                        Toast.makeText(context, "이미 권한이 허용되어 있습니다.", Toast.LENGTH_SHORT).show()
                    } else permissionLauncher.launch(blePermissions())
                },
                modifier = Modifier.weight(1f)
            ) { Text("권한 요청") }

            // 2️⃣ 스캔 시작
            Button(
                onClick = {
                    if (hasBlePermissions(context)) {
                        vm.startScan()
                        showDialog = true
                        autoCloseTimer = true
                    } else {
                        Toast.makeText(context, "권한을 먼저 허용해주세요.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = scanState !is ScanState.Scanning,
                modifier = Modifier.weight(1f)
            ) { Text("스캔 시작") }

//            // 3️⃣ 중지
//            Button(
//                onClick = {
//                    vm.stopScan()
//                    showDialog = false
//                },
//                enabled = scanState is ScanState.Scanning,
//                modifier = Modifier.weight(1f)
//            ) { Text("중지") }

            Button(
                onClick = { vm.sendTestData() },
                modifier = Modifier.weight(1f)
            ) {
                Text("데이터 전송")
            }

            Button(
                onClick = { vm.disconnect() },
                modifier = Modifier.weight(1f)
            ) {
                Text("연결 끊기")
            }
        }
    }

    // 🔹 스캔 결과 팝업 다이얼로그
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                vm.stopScan()
                showDialog = false
                autoCloseTimer = false
            },
            title = { Text("주변 BLE 기기") },
            text = {
                if (devices.isEmpty()) {
                    Text("검색 중... ")
                } else {
                    Column {
                        devices.forEach { dev ->
                            Text(
                                text = "${dev.name ?: "Unknown"} (${dev.address})",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vm.connectToDevice(dev)
                                        vm.stopScan()
                                        showDialog = false
                                        autoCloseTimer = false
                                    }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.stopScan()
                    showDialog = false
                    autoCloseTimer = false
                }) {
                    Text("닫기")
                }
            }
        )
    }
}

