package com.example.demo.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BleViewModel(private val context: Context) : ViewModel() {

    // ✅✅✅-----------------리스트 추가하기
//    private val _fileList = MutableStateFlow<List<FileEntry>>(emptyList())
//    val fileList: StateFlow<List<FileEntry>> = _fileList

    // ✅ ESP32 쪽에서 사용한 Service / Characteristic UUID 로 교체해야 함
    private val SERVICE_UUID = java.util.UUID.fromString("e49a25f8-f69a-11e8-8eb2-f2801f1b9fd1")
    private val WRITE_CHAR_UUID = java.util.UUID.fromString("e49a25e0-f69a-11e8-8eb2-f2801f1b9fd1")  // RX (Write)
    private val NOTIFY_CHAR_UUID = java.util.UUID.fromString("e49a28e1-f69a-11e8-8eb2-f2801f1b9fd1") // TX (Notify)

    // ✅ GATT 연결 객체 & write용 characteristic
    private var gatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null

    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _devices = MutableStateFlow<List<BleDevice>>(emptyList())
    val devices: StateFlow<List<BleDevice>> = _devices

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState

    private var scanCallback: ScanCallback? = null
    private var timeoutJob: Job? = null

    private val eqWriteQueue: ArrayDeque<ByteArray> = ArrayDeque()
    @Volatile private var eqWorkerRunning = false

//    /** 우선 UI 확인용 더미 데이터 넣기 */
//    fun putDummyList() {
//        _fileList.value = listOf(
//            FileEntry(0, "MUSIC01.MP3"),
//            FileEntry(1, "MUSIC02.MP3"),
//            FileEntry(2, "LIVE_TRACK.AAC")
//        )
//    }
//
//    /** 나중에 실제로 MCU에 ‘리스트 주세요(0xF1)’ 보낼 함수 자리 */
//    fun requestFileList() {
//        _fileList.value = emptyList()
//        // TODO: BLE write(byteArrayOf(0xF1)) 붙일 예정
//    }
//
//    /** 항목을 탭하면 MCU에 ‘재생(0xF2)’ 보낼 함수 자리 */
//    fun playFile(fileNum: Int) {
//        // TODO: BLE write(byteArrayOf(0xF2, fileNum.toByte(), fileNum.toByte())) 붙일 예정
//    }

    /** 🔹 스캔 시작 (10초 후 자동 중지) */
    @SuppressLint("MissingPermission")
    fun startScan() {
        val btAdapter = adapter ?: run {
            Log.e("BLE_test", "❌ BluetoothAdapter is null — BLE not supported on this device.")
            Toast.makeText(context, "이 기기는 BLE를 지원하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ 권한 체크 (Android 12+는 위치 권한 요구 X)
        val requiredPerms = buildList {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            // Android 11 이하만 위치 권한 필요
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.R) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        val missing = requiredPerms.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            Log.w("BLE_test", "⚠️ 권한 미허용: $missing")
            Toast.makeText(context, "근처 기기 권한을 허용해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val scanner = btAdapter.bluetoothLeScanner ?: run {
            Log.e("BLE_test", "❌ BluetoothLeScanner is null — BLE 스캐너 생성 실패.")
            Toast.makeText(context, "BLE 스캔을 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (_scanState.value == ScanState.Scanning) {
            Log.w("BLE_test", "⚠️ 이미 스캔 중입니다.")
            return
        }

        // 이전 콜백 중복 방지
        scanCallback?.let {
            Log.w("BLE_test", "⚠️ 기존 스캔 중지 후 새로 시작.")
            scanner.stopScan(it)
        }

        val discovered = LinkedHashMap<String, BleDevice>()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(type: Int, result: ScanResult) {
                val name = result.device.name ?: result.scanRecord?.deviceName ?: "Unknown"
                val addr = result.device.address ?: return
                val rssi = result.rssi
                discovered[addr] = BleDevice("$name (RSSI:$rssi)", addr)
                _devices.value = discovered.values.toList()
                Log.d("BLE_test", "📡 발견: $name [$addr] RSSI:$rssi")
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                for (r in results) {
                    val name = r.device.name ?: r.scanRecord?.deviceName ?: "Unknown"
                    val addr = r.device.address ?: continue
                    discovered[addr] = BleDevice("$name (RSSI:${r.rssi})", addr)
                }
                _devices.value = discovered.values.toList()
                Log.d("BLE_test", "📦 배치 스캔 결과: ${results.size}개")
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("BLE_test", "❌ 스캔 실패 (errorCode=$errorCode)")
                _scanState.value = ScanState.Error("스캔 실패 ($errorCode)")
                when (errorCode) {
                    1 -> Log.e("BLE_test", "SCAN_FAILED_ALREADY_STARTED — 이미 스캔 중")
                    2 -> Log.e("BLE_test", "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED — BLE 권한/시스템 문제")
                    3 -> Log.e("BLE_test", "SCAN_FAILED_FEATURE_UNSUPPORTED — 기기에서 BLE 스캔 미지원")
                    4 -> Log.e("BLE_test", "SCAN_FAILED_INTERNAL_ERROR — 시스템 BLE 스택 오류 (재부팅 필요)")
                    5 -> Log.e("BLE_test", "SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES — BLE 버퍼 부족")
                }
                stopScan()
            }
        }

        // 스캔 시작
        scanCallback = callback
        scanner.startScan(null, settings, callback)
        _scanState.value = ScanState.Scanning
        Log.i("BLE_test", "✅ BLE 스캔 시작됨")

        // 🔸 10초 후 자동 중지
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(10_000)
            if (_scanState.value == ScanState.Scanning) {
                stopScan()
                if (_devices.value.isEmpty()) {
                    _scanState.value = ScanState.Error("10초 내 기기를 찾지 못했습니다.")
                    Log.w("BLE_test", "⏰ 10초 동안 BLE 기기 발견되지 않음")
                    Toast.makeText(context, "주변에 BLE 기기를 찾지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            gatt?.disconnect()
            gatt?.close()
            Log.i("BLE_test", "🔌 GATT 연결 끊김")
        } catch (e: Exception) {
            Log.e("BLE_test", "❌ disconnect 중 오류: ${e.message}")
        } finally {
            gatt = null
            writeChar = null
        }
    }

    /** 🔹 스캔 중지 */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        val btAdapter = adapter ?: return
        val scanner = btAdapter.bluetoothLeScanner ?: return
        scanCallback?.let {
            scanner.stopScan(it)
            Log.i("BLE_test", "🛑 BLE 스캔 중지됨")
        }
        scanCallback = null
        timeoutJob?.cancel()
        _scanState.value = ScanState.Idle
    }

    /** 🔹 기기 연결 (GATT 콜백 포함) */
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BleDevice) {
        val btDevice = adapter?.getRemoteDevice(device.address)
        if (btDevice == null) {
            Log.e("BLE_test", "❌ getRemoteDevice 실패: ${device.address}")
            return
        }

        Log.d("BLE_test", "📡 연결 시도: ${device.name} (${device.address})")

        gatt = btDevice.connectGatt(context, false, object : BluetoothGattCallback() {

            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.i("BLE_test", "✅ GATT 연결 성공, 서비스 탐색 시작")
                        this@BleViewModel.gatt = gatt
                        gatt.discoverServices()
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.w("BLE_test", "🛑 GATT 연결 해제됨")
                        this@BleViewModel.gatt = null
                        writeChar = null
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e("BLE_test", "❌ 서비스 탐색 실패: status=$status")
                    return
                }

                Log.i("BLE_test", "✅ 서비스 탐색 완료")

                val service = gatt.getService(SERVICE_UUID)
                if (service == null) {
                    Log.e("BLE_test", "❌ 지정한 SERVICE_UUID를 찾지 못함")
                    return
                }

                val characteristic = service.getCharacteristic(WRITE_CHAR_UUID)
                if (characteristic == null) {
                    Log.e("BLE_test", "❌ 지정한 WRITE_CHAR_UUID를 찾지 못함")
                    return
                }

                writeChar = characteristic
                Log.i("BLE_test", "✅ write용 characteristic 연결 완료")
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i("BLE_test", "📤 Characteristic write 성공")
                } else {
                    Log.e("BLE_test", "❌ Characteristic write 실패: status=$status")
                }
            }
        })
    }

    /** 🔹 ESP32로 테스트 데이터 전송 ("Hello") */
    @SuppressLint("MissingPermission")
    fun sendTestData() {
        val gatt = this.gatt
        val characteristic = this.writeChar

        if (gatt == null || characteristic == null) {
            Toast.makeText(context, "먼저 기기에 연결 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            Log.w("BLE_test", "⚠️ GATT 또는 writeChar가 null — 아직 연결/서비스 준비 안 됨")
            return
        }

        val data = "Hello".toByteArray(Charsets.UTF_8)
        characteristic.value = data
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

        val result = gatt.writeCharacteristic(characteristic)
        Log.d("BLE_test", "📡 writeCharacteristic 호출 결과: $result, data=${data.contentToString()}")
    }

    // BleViewModel 내부에 추가

    /** checksum = ((Header + ID + Data) & 0xFF) XOR 0xFF */
    private fun checksum(header: Int, id: Int, data: Int): Int {
        val low = (header + id + data) and 0xFF
        return (low xor 0xFF) and 0xFF
    }

    /** 프레임 생성: [Header, ID, Data, Checksum] */
    fun buildFrame(id: Int, data: Int, header: Int = 0xEC): ByteArray {
        val cs = checksum(header, id, data)
        return byteArrayOf(header.toByte(), id.toByte(), data.toByte(), cs.toByte())
    }

    /** 실제 GATT write */
    @SuppressLint("MissingPermission")
    private fun write(bytes: ByteArray): Boolean {
        val g = gatt ?: return false
        val ch = writeChar ?: return false
        ch.value = bytes
        ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val ok = g.writeCharacteristic(ch)
        Log.d("BLE_test", "📤 write: ${bytes.joinToString(" ") { "%02X".format(it) }} (ok=$ok)")
        if (!ok) Toast.makeText(context, "전송 실패: 연결/서비스 확인", Toast.LENGTH_SHORT).show()
        return ok
    }

    /** ID/데이터만 주면 헤더/체크섬 포함해 전송 */
    fun sendCommand(id: Int, data: Int, header: Int = 0xEC): Boolean {
        val frame = buildFrame(id, data, header)
        return write(frame)
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCleared() {
        stopScan()
        gatt?.close()
        gatt = null
        writeChar = null
        super.onCleared()
    }





    // 큐 워커 시작 (이미 돌고 있으면 재시작 안 함)
    private fun startEqQueueWorker() {
        if (eqWorkerRunning) return
        eqWorkerRunning = true
        viewModelScope.launch {
            while (eqWriteQueue.isNotEmpty()) {
                val frame = eqWriteQueue.removeFirstOrNull() ?: break
                val ok = safeWriteWithDelay(frame)
                if (!ok) {
                    // 연결/서비스 미준비 혹은 BUSY 시 약간 대기 후 재시도
                    delay(40)
                    eqWriteQueue.addFirst(frame)
                } else {
                    // 너무 과도한 write 방지를 위해 소량 슬립
                    delay(30)
                }
            }
            eqWorkerRunning = false
        }
    }

    // 즉시 write(콜백 의존 없이 지연만으로 직렬화)
    @SuppressLint("MissingPermission")
    private fun safeWriteWithDelay(bytes: ByteArray): Boolean {
        val g = gatt ?: return false
        val ch = writeChar ?: return false
        ch.value = bytes
        ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val ok = g.writeCharacteristic(ch)
        Log.d("BLE_eq", "➡ EQ write ${bytes.joinToString(" ") { "%02X".format(it) }} ok=$ok")
        return ok
    }

    // ✅ Equalizer에서만 사용할 전용 큐 API
    fun sendCommandQueued(id: Int, data: Int, header: Int = 0xEC) {
        val frame = buildFrame(id, data, header)  // 네가 이미 가진 buildFrame 사용
        eqWriteQueue.addLast(frame)

        // gatt / writeChar가 아직 null이면, 큐에 쌓아두기만 하고 워커는 나중에 다시 호출
        if (gatt == null || writeChar == null) {
            Log.w("BLE_eq", "⏳ GATT/Characteristic 미준비 — EQ 큐에 적재만 함")
            return
        }
        startEqQueueWorker()
    }
}
