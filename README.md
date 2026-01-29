# MP3 Player BLE Control App (Android)

BLE 데이터 프로토콜을 기반으로 MP3 Player 기능을 제어하는 안드로이드 앱입니다.  
실제 보드가 아닌 테스트용으로 ESP32를 구매하여 BLE 서버로 구성하고 송수신 테스트를 진행했습니다.

## 주요 기능
- BLE 스캔 및 기기 연결(GATT)
- 버튼/슬라이더 입력 → 프로토콜 프레임 생성 후 전송(Write)
- 기기 상태 수신(Notify) → 프레임 검증 후 UI 상태 동기화
- (옵션) 수신 데이터 Room DB 저장(Upsert)로 리스트 관리

## 통신 방식 요약
- 앱(Central) ↔ ESP32(Peripheral) 간 GATT 기반 통신
- Service UUID 아래 두 개 Characteristic 사용
  - Write Characteristic: 앱 → 기기 명령 전송
  - Notify Characteristic: 기기 → 앱 상태/데이터 알림

## 데이터 프레임
### 앱 → 기기 (Write, 4 bytes)
[Header][ID][DATA][Checksum]
- Checksum = ((Header + ID + DATA) & 0xFF) XOR 0xFF

### 기기 → 앱 (Notify)
- 파일 리스트 프레임(35 bytes): [0xF1][FileNum][FileName(32)][Checksum]
- 상태 프레임(4 bytes): [0xED][ID][DATA][Checksum]

## UI 동작
- UI 조작(버튼/스텝퍼/슬라이더) 시 ViewModel에서 프레임을 생성(buildFrame)하고 writeCharacteristic()로 전송합니다.
- Notify로 수신된 데이터는 버퍼에 누적한 뒤, 헤더 정렬/체크섬 검증 후 파싱하여 UI 상태를 갱신합니다.

## 테스트 환경
- BLE 테스트 장치: ESP32 (Arduino BLE Server)
- 로그 확인: Android Logcat + ESP32 Serial Monitor
