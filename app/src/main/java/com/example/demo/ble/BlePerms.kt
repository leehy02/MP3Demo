package com.example.demo.ble

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat

/** ✅ 현재 OS 버전에 맞는 BLE 권한 목록 반환 */
fun blePermissions(): Array<String> =
    if (android.os.Build.VERSION.SDK_INT >= 31) { // Android 12+
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

/** ✅ 모든 BLE 권한이 허용되었는지 확인 */
fun hasBlePermissions(context: Context): Boolean =
    blePermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/** ✅ 위치 서비스 활성화 여부 (Android 12 이하에서만 필요) */
fun isLocationServiceOn(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= 31) return true
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}
