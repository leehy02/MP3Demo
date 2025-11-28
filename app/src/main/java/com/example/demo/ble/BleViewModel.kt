package com.example.demo.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
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
import java.util.UUID
import com.example.demo.data.AppDatabase
import com.example.demo.data.FileDao
import com.example.demo.data.FileEntity
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers

class BleViewModel(private val context: Context) : ViewModel() {

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

    // ✅✅✅-----------------notify
    private val recvBuffer = mutableListOf<Byte>()

    // ✅✅✅✅✅✅-----------------Room DB & DAO
    private val db: AppDatabase by lazy { AppDatabase.getInstance(context.applicationContext) }
    private val fileDao: FileDao by lazy { db.fileDao() }

//    val fileList: StateFlow<List<FileEntry>> =
//        fileDao.getAll()
//            .map { list -> list.map { e -> FileEntry(number = e.fileNumber, name = e.fileName) } }
//            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // [ADD] fileList 생성부 체인 중간에 onEach로 로그 추가하면 디버깅 편함
    val fileList: StateFlow<List<FileEntry>> =
        fileDao.getAll()
            .onEach { Log.d("BLE_file", "DB emit rows=${it.size}") }   // [ADD]
            .map { list -> list.map { e -> FileEntry(e.fileNumber, e.fileName) } }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())



    // 준비상태 플래그 (버튼 눌러 F3 보낼 때 체크)
    val readyForList = AtomicBoolean(false)

    private fun ByteArray.hex(): String = joinToString(" ") { "%02X".format(it) }




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

            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "연결이 끊어졌습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }

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

                        viewModelScope.launch(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "${device.name} 연결되었습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }



                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.w("BLE_test", "🛑 GATT 연결 해제됨")
                        this@BleViewModel.gatt = null
                        writeChar = null

                        viewModelScope.launch(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "연결이 끊어졌습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

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



                // ✅✅✅-----------------notify
                // ✅ NOTIFY/INDICATE 구독 + MTU/ConnPriority + 준비 플래그
                val notifyChar = service.getCharacteristic(NOTIFY_CHAR_UUID)
                if (notifyChar == null) {
                    Log.e("BLE_test", "❌ NOTIFY_CHAR_UUID characteristic 못 찾음")
                    return
                }

                // 1) 앱 레벨 알림 켜기
                gatt.setCharacteristicNotification(notifyChar, true)

                // 2) CCCD(0x2902) 값 결정: Indicate 지원시 INDICATE, 아니면 NOTIFY
                val supportsIndicate = (notifyChar.properties and
                        BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                val cccd = notifyChar.getDescriptor(cccdUuid)
                if (cccd == null) {
                    Log.w("BLE_file", "⚠ notifyChar에 CCCD 디스크립터가 없음")
                } else {
                    cccd.value = if (supportsIndicate)
                        BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    else
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

                    val ok = gatt.writeDescriptor(cccd)
                    Log.d("BLE_file", "CCCD write ok=$ok, mode=${if (supportsIndicate) "INDICATE" else "NOTIFY"}")
                }

                // 3) MTU/연결우선순위 요청 (요청 직후 바로 F3 보내지 말 것!)
                gatt.requestMtu(64)  // 35B 프레임 한 번에 수신
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)

                // 4) 준비 플래그 (onDescriptorWrite / onMtuChanged에서 true로 세팅 추천)
                readyForList.set(false) // AtomicBoolean을 ViewModel 멤버로 두자
            }

            // ✅✅✅-----------------notify
//            override fun onCharacteristicChanged(
//                gatt: BluetoothGatt,
//                characteristic: BluetoothGattCharacteristic,
//                value: ByteArray
//            ) {
//                // super.onCharacteristicChanged(gatt, characteristic, value) // 굳이 호출 안 해도 됨
//
//                if (characteristic.uuid == NOTIFY_CHAR_UUID) {
//                    val data = value
//                    Log.d("BLE_file", "RX(new): ${data.joinToString(" ") { "%02X".format(it) }}")
//                    handleNotify(data)
//                }
//            }

            // [CHG] 콜백 내부의 onCharacteristicChanged 를 아래로 교체
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                if (characteristic.uuid == NOTIFY_CHAR_UUID) {
                    Log.d("BLE_file", "RX chunk (${value.size}B): ${value.hex()}")
                    handleNotify(value)
                }
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

//    //리스트 테스트용
//    fun buildFrame_test(id: Int, data: Int, header: Int = 0xF3): ByteArray {
//        val cs = checksum(header, id, data)
//        return byteArrayOf(header.toByte(), id.toByte(), data.toByte(), cs.toByte())
//    }


//    fun sendCommand_test(id: Int, data: Int, header: Int = 0xF3): Boolean {
//        val frame = buildFrame_test(id, data, header)
//        return write(frame)
//    }


    /** checksum = ((Header + ID + Data) & 0xFF) XOR 0xFF */
    private fun checksum2(header: Int, num: Int): Int {
        val low = (header + num) and 0xFF
        return (low xor 0xFF) and 0xFF
    }

    fun buildFrame2(num: Int, header: Int = 0xF2): ByteArray {
        val cs = checksum2(header, num)
        return byteArrayOf(header.toByte(), num.toByte(), cs.toByte())
    }

    fun sendCommand2(num: Int, header: Int = 0xF2): Boolean {
        val frame = buildFrame2(num, header)
        return write(frame)
    }

    fun playFile(fileNum: Int) {
        sendCommand2(fileNum)   // <- F2, fileNum, checksum 전송
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



    // ✅✅✅-----------------notify
//    private fun handleNotify(bytes: ByteArray) {
//        // 1) 버퍼에 계속 쌓기
//        recvBuffer.addAll(bytes.toList())
//
//        // 2) 35바이트(1+1+32+1)씩 프레임 파싱
//        val FRAME_SIZE = 35
//        while (recvBuffer.size >= FRAME_SIZE) {
//            val frame = recvBuffer.take(FRAME_SIZE).toByteArray()
//            repeat(FRAME_SIZE) { recvBuffer.removeAt(0) }
//
//            Log.d("BLE_file", "parseFileFrame() 호출 직전, frameSize=${frame.size}")
//            parseFileFrame(frame)
//        }
//    }

    // [CHG] 기존 handleNotify 전체를 아래로 교체
    private fun handleNotify(bytes: ByteArray) {
        try {
            // 버퍼에 누적
            recvBuffer.addAll(bytes.asList())
            Log.d("BLE_file", "handleNotify: add=${bytes.size}B, buf=${recvBuffer.size}B")

            val FRAME = 35

            // F1 헤더 정렬: F1 나올 때까지 앞 바이트 드롭
            var dropped = 0
            while (recvBuffer.isNotEmpty() && (recvBuffer[0].toInt() and 0xFF) != 0xF1) {
                recvBuffer.removeAt(0)
                dropped++
            }
            if (dropped > 0) Log.w("BLE_file", "drop $dropped B (seek F1), buf=${recvBuffer.size}B")

            // 35바이트 프레임이 준비될 때까지 반복 파싱
            while (recvBuffer.size >= FRAME) {
                val frame = recvBuffer.subList(0, FRAME).toByteArray()

                val header = frame[0].toInt() and 0xFF
                if (header != 0xF1) {
                    Log.w("BLE_file", "unexpected header=0x${"%02X".format(header)} → resync")
                    recvBuffer.removeAt(0)
                    continue
                }

                // 체크섬: 앞 34바이트 합의 LSB
                val calc = (0 until 34).fold(0) { acc, i -> acc + (frame[i].toInt() and 0xFF) } and 0xFF
                val recv = frame[34].toInt() and 0xFF

                if (calc != recv) {
                    Log.e("BLE_file", "checksum mismatch: calc=${"%02X".format(calc)} recv=${"%02X".format(recv)} frame=${frame.hex()}")
                    // 한 바이트 밀고 다시 정렬 시도
                    recvBuffer.removeAt(0)
                    continue
                } else {
                    Log.d("BLE_file", "valid frame (35B): ${frame.hex()}")
                    parseFileFrame(frame) // 안전 파싱 (아래 교체본)
                }

                // 소비
                repeat(FRAME) { recvBuffer.removeAt(0) }
                Log.d("BLE_file", "consume 35B → remain=${recvBuffer.size}B")
            }
        } catch (t: Throwable) {
            Log.e("BLE_file", "❌ handleNotify crash: ${t.message}", t)
            recvBuffer.clear() // 복구용 초기화
        }
    }


    // 실제 파싱
    // ✅✅✅✅✅✅-----------------Room DB & DAO
//    private fun parseFileFrame(frame: ByteArray) {
////        if (frame.size != 35) return
////        val header = frame[0].toInt() and 0xFF
////        if (header != 0xF1) return   // DSP→APP 파일리스트 프레임만 처리
////
////        val fileNum = frame[1].toInt() and 0xFF
////
////        val nameBytes = frame.copyOfRange(2, 34) // 32바이트
////        val rawName = nameBytes.takeWhile { it != 0.toByte() }.toByteArray()
////        val fileName = rawName.toString(Charsets.UTF_8)
////
////        val checksum = frame[34].toInt() and 0xFF
////        // TODO: checksum 검증 로직 (스펙에 맞게 나중에 추가)
////
////        // 리스트에 추가
////        val old = _fileList.value
////        _fileList.value = old + FileEntry(fileNum, fileName)
//
//        Log.d(
//            "BLE_file",
//            "parseFileFrame() 시작, size=${frame.size}, header=${frame[0].toInt() and 0xFF}"
//        )
//
//        if (frame.size != 35) return
//        val header = frame[0].toInt() and 0xFF
//        if (header != 0xF1) return   // DSP→APP 파일리스트 프레임만 처리
//
//        val fileNum = frame[1].toInt() and 0xFF
//        val nameBytes = frame.copyOfRange(2, 34)
//        val rawName = nameBytes.takeWhile { it != 0.toByte() }.toByteArray()
//        val fileName = rawName.toString(Charsets.UTF_8)
//        val checksum = frame[34].toInt() and 0xFF
//        Log.d("BLE_file", "파싱 결과: num=$fileNum, name='$fileName', checksum=$checksum")
//
//        // ✅ DB에만 저장하면, fileList(StateFlow)가 자동 갱신됨
//        viewModelScope.launch {
//            try {
//                fileDao.insertFile(FileEntity(fileNumber = fileNum, fileName = fileName))
//                Log.d("BLE_file", "📦 Room 저장 완료: #$fileNum $fileName")
//            } catch (e: Exception) {
//                Log.e("BLE_file", "❌ Room insert 실패: ${e.message}")
//            }
//        }
//    }

    // [CHG] 기존 parseFileFrame 전체를 아래로 교체
    private fun parseFileFrame(frame: ByteArray) {
        try {
            if (frame.size != 35) {
                Log.w("BLE_file", "parse skip: size=${frame.size}")
                return
            }
            val header = frame[0].toInt() and 0xFF
            if (header != 0xF1) {
                Log.w("BLE_file", "parse skip: header=0x${"%02X".format(header)}")
                return
            }

            val fileNum = frame[1].toInt() and 0xFF

            // 이름(32B)에서 0-terminated 안전 추출
            val nameBytes = frame.copyOfRange(2, 34)
            val zero = nameBytes.indexOf(0)
            val real = if (zero >= 0) nameBytes.copyOf(zero) else nameBytes

            val fileName = try {
                String(real, Charsets.UTF_8)
            } catch (_: Throwable) {
                runCatching { String(real, Charsets.US_ASCII) }.getOrElse {
                    real.hex() // 최후: hex로라도 표현
                }
            }

            val checksum = frame[34].toInt() and 0xFF
            Log.d("BLE_file", "parse OK → num=$fileNum, name='$fileName', cs=${"%02X".format(checksum)}")

            // DB 저장(Upsert) — IO에서 수행
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    fileDao.upsert(FileEntity(fileNumber = fileNum, fileName = fileName))
                    Log.d("BLE_file", "Room upsert OK: #$fileNum '$fileName'")
                } catch (e: Exception) {
                    Log.e("BLE_file", "Room upsert FAIL: ${e.message}", e)
                }
            }
        } catch (t: Throwable) {
            Log.e("BLE_file", "❌ parseFileFrame crash: ${t.message}", t)
        }
    }


    /** Room의 파일 목록 전체 삭제 */
    fun clearFileEntries() {
        viewModelScope.launch {
            try {
                fileDao.clear()
                Log.d("BLE_file", "✅ DB cleared")
            } catch (e: Exception) {
                Log.e("BLE_file", "❌ clearFileEntries failed: ${e.message}")
            }
        }
    }
}
