package com.example.demo.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.example.demo.ble.BleViewModel
import com.example.demo.ble.FileEntry
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FileListScreen(vm: BleViewModel) {
    val files by vm.fileList.collectAsState(initial = emptyList())
    var showConfirm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Spacer(Modifier.height(15.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showConfirm = true }) { Text("File Reset") }
        }

        Spacer(Modifier.height(12.dp))
        Text("파일 목록 (${files.size})", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),              // ← 남은 공간 전부 차지 → 스크롤 보장
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 24.dp) // 마지막 아이템 여유
        ) {
            items(items = files, key = { it.number }) { item ->
                FileRow(entry = item, onClick = { vm.playFile(item.number) })
                Divider()
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            confirmButton = { TextButton({ showConfirm = false; vm.clearFileEntries() }) { Text("삭제") } },
            dismissButton = { TextButton({ showConfirm = false }) { Text("취소") } },
            title = { Text("파일 초기화") },
            text = { Text("Room에 저장된 파일 목록을 모두 삭제할까요?") }
        )
    }
}


@Composable
private fun FileRow(entry: FileEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(

            ) }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text("#${entry.number}", modifier = Modifier.width(56.dp))
        Text(entry.name)
    }
}


//@Composable
//fun FileListScreen(vm: BleViewModel) {
//    val files by vm.fileList.collectAsState(initial = emptyList())
//
//    Column(Modifier.fillMaxSize().padding(16.dp)) {
//
//        Spacer(Modifier.height(15.dp))
//
//        Row(
//            Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
////            // ✅ 새로 받기: DB 비우고 → F3 트리거 전송 → (화면 열기/유지)
////            Button(onClick = {
////                vm.clearFileEntries()
////                if (vm.readyForList.get()) {
////                    vm.sendCommand_test(0x01, 0x01) // 헤더 기본값 0xF3 → MCU가 데모리스트 전송
////                    //showFileList = true
////                } else {
////                    // 준비가 아직이면 약간 딜레이 후 재시도
////                    vm.viewModelScope.launch {
////                        delay(200)
////                        if (vm.readyForList.get()) vm.sendCommand_test(0x01, 0x01)
////                    }
////                }
////            }) {
////                Text("리스트 새로 받기 (F3)")
////            }
////
////
//            // ✅ 전부 삭제: DB만 비움 (UI는 Flow라 자동 갱신)
//            Button(onClick = {
//                vm.clearFileEntries()
//            }) {
//                Text("File Reset")
//            }
//        }
//
//        Spacer(Modifier.height(5.dp))
//
//        Spacer(Modifier.height(12.dp))
//
//        Text("파일 목록 (${files.size})", style = MaterialTheme.typography.titleMedium)
//
//        Spacer(Modifier.height(8.dp))
//
//        LazyColumn(
//            modifier = Modifier.fillMaxSize(),
//            verticalArrangement = Arrangement.spacedBy(4.dp)
//        ) {
//            items(files) { item ->
//                FileRow(
//                    entry = item,
//                    onClick = { vm.playFile(item.number) }
//                )
//                Divider()
//            }
//        }
//    }
//}
//
//
//@Composable
//private fun FileRow(entry: FileEntry, onClick: () -> Unit) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { onClick() }
//            .padding(horizontal = 12.dp, vertical = 10.dp)
//    ) {
//        Text("#${entry.number}", modifier = Modifier.width(56.dp))
//        Text(entry.name)
//    }
//}
