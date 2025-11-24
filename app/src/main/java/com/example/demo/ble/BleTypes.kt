package com.example.demo.ble

/** BLE 기기 데이터 */
data class BleDevice(
    val name: String?,
    val address: String
)

/** 스캔 상태 */
sealed class ScanState {
    data object Idle : ScanState()
    data object Scanning : ScanState()
    data class Error(val msg: String) : ScanState()
}