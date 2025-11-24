package com.example.demo.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.demo.ble.BleViewModel
import com.example.demo.ble.FileEntry
import androidx.navigation.NavController

@Composable
fun FileListScreen(vm: BleViewModel) {
    val files by vm.fileList.collectAsState()

    // ✅ 화면 들어오면 즉시 더미 넣기(테스트용)
    LaunchedEffect(Unit) {
        vm.putDummyList()            // <-- 지금은 이걸로 즉시 목록 보이게
        // 나중에 실제 연결되면 아래로 교체:
        // vm.requestFileList()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Spacer(Modifier.height(15.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { vm.putDummyList() }) { Text("더미 넣기") }
            Button(onClick = { vm.requestFileList() }) { Text("MCU에 리스트 요청") }
        }

        Spacer(Modifier.height(5.dp))

        Spacer(Modifier.height(12.dp))

        Text("파일 목록 (${files.size})", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(files) { item ->
                FileRow(
                    entry = item,
                    onClick = { vm.playFile(item.number) }
                )
                Divider()
            }
        }
    }
}


@Composable
private fun FileRow(entry: FileEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text("#${entry.number}", modifier = Modifier.width(56.dp))
        Text(entry.name)
    }
}
